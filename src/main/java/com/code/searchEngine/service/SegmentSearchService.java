package com.code.searchEngine.service;

import com.code.searchEngine.dto.SearchResponse;
import com.code.searchEngine.dto.SearchResult;
import com.code.searchEngine.index.segment.SegmentManager;
import com.code.searchEngine.index.segment.SegmentPosting;
import com.code.searchEngine.index.segment.SegmentReader;
import com.code.searchEngine.model.DocLocation;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.repository.DocLocationRepo;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.utils.Analyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SegmentSearchService {

    private static final double K1 = 1.5;

    private static final double TITLE_WEIGHT = 3.0;
    private static final double TITLE_B = 0.3;
    private static final double TEXT_WEIGHT = 1.0;
    private static final double TEXT_B = 0.75;

    private final Analyzer analyzer;
    private final SegmentManager segmentManager;
    private final PageMetadataRepo pageMetadataRepo;
    private final DocLocationRepo docLocationRepo;

    private record Stats(long totalDocs, double avgTitleLength, double avgTextLength, Map<String, Integer> docFreq) {}

    public SearchResponse handleSearch( String rawQuery, int limit ){
        long start = System.currentTimeMillis();

        boolean isPhrase = rawQuery.length() > 2 && rawQuery.startsWith("\"") && rawQuery.endsWith("\"");

        List<SearchResult> results = isPhrase ? phraseSearch(rawQuery, limit) : search(rawQuery, limit);

        return new SearchResponse(rawQuery, results.size(), System.currentTimeMillis() - start, results);
    }

    public List<SearchResult> search(String query, int limit){
        Set<String> terms = new HashSet<>(analyzer.tokenize(query));
        if (terms.isEmpty()) return List.of();

        List<SegmentReader> segments = segmentManager.getActiveSegments();
        Stats stats = gatherStats(terms, segments);
        if (stats.totalDocs() == 0) return List.of();

        Map<UUID, Double> scores = score(terms, segments, stats, null);
        return topResults(scores, terms, limit);
    }

    public List<SearchResult> phraseSearch(String rawQuery, int limit){
        String phraseText = rawQuery.substring(1, rawQuery.length() - 1);
        Set<String> phraseTerms = new HashSet<>(analyzer.tokenize(phraseText));
        if(phraseTerms.size() < 2) return search(phraseText, limit);

        List<SegmentReader> segments = segmentManager.getActiveSegments();
        Set<UUID> matches = findPhraseMatches(phraseTerms, segments);
        if (matches.isEmpty()) return List.of();

        Set<String> terms = new HashSet<>(phraseTerms);
        Stats stats = gatherStats(terms, segments);
        if (stats.totalDocs() == 0) return List.of();

        Map<UUID, Double> scores = score(terms, segments, stats, matches);
        return topResults(scores, terms, limit);
    }

    public Set<UUID> findPhraseMatches(Set<String> terms, List<SegmentReader> segments) {
        Set<UUID> matches = new HashSet<>();

        for (SegmentReader seg : segments) {
            List<Map<UUID, int[]>> positionsByTerm = new ArrayList<>();
            boolean segmentCanMatch = true;

            for (String term : terms) {
                Map<UUID, int[]> byDoc = new HashMap<>();
                for(SegmentPosting p: seg.getPostings(term)){
                    byDoc.put(p.docId(), p.textPositions());
                }
                if (byDoc.isEmpty()) {
                    segmentCanMatch = false;
                    break;
                }
                positionsByTerm.add(byDoc);
            }
            if (!segmentCanMatch) continue;

            for(UUID docId: positionsByTerm.get(0).keySet()){
                if (docHasPhrase(docId, positionsByTerm))
                    matches.add(docId);
            }
        }
        return matches;
    }

    private boolean docHasPhrase(UUID docId, List<Map<UUID, int[]>> positionsByTerm) {
        int[][] positions = new int[positionsByTerm.size()][];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = positionsByTerm.get(i).get(docId);
            if (positions[i] == null) return false;
        }

        for (int startPos : positions[0]) {
            boolean allFollow = true;
            for (int i = 1; i < positions.length; i++) {
                if (Arrays.binarySearch(positions[i], startPos + i) < 0) {
                    allFollow = false;
                    break;
                }
            }
            if (allFollow) return true;
        }
        return false;
    }

    private Stats gatherStats(Set<String> terms, List<SegmentReader> segments) {
        long totalDocs = 0;
        long totalTitleTokens = 0, totalTextTokens = 0;
        Map<String, Integer> docFreq = new HashMap<>();

        for (SegmentReader seg : segments) {
            totalDocs += seg.docCount();
            totalTitleTokens += (long) (seg.averageTitleLength() * seg.docCount());
            totalTextTokens += seg.totalTextTokens();

            for (String term : terms) docFreq.merge(term, seg.docFrequency(term), Integer::sum);
        }

        double avgTitle = totalDocs == 0 ? 0.0 : (double) totalTitleTokens / totalDocs;
        double avgText = totalDocs == 0 ? 0.0 : (double) totalTextTokens / totalDocs;
        return new Stats(totalDocs, avgTitle, avgText, docFreq);
    }

    private Map<UUID, Double> score(Set<String> terms, List<SegmentReader> segments, Stats stats, Set<UUID> candidates) {
        Map<UUID, Double> scores = new HashMap<>();

        Set<UUID> candidateDocIds = new HashSet<>();

        for (String term : terms) {
            for (SegmentReader seg : segments) {
                for (SegmentPosting p : seg.getPostings(term)) candidateDocIds.add(p.docId());

            }
        }

        Map<UUID, Long> liveSegmentByDoc = docLocationRepo.findByPageIdIn(candidateDocIds).stream()
                .collect(Collectors.toMap(DocLocation::getPageId, DocLocation::getSegmentId));

        for (String term : terms) {
            int df = stats.docFreq().getOrDefault(term, 0);
            if(df==0) continue;

            double idf = Math.log(1 + (stats.totalDocs() - df + 0.5) / (df + 0.5));

            for (SegmentReader seg : segments) {
                for (SegmentPosting p : seg.getPostings(term)) {
                    if (candidates != null && !candidates.contains(p.docId())) continue;

                    Long liveSegment = liveSegmentByDoc.get(p.docId());
                    if (liveSegment == null || liveSegment != seg.getSegmentId()) continue;

                    double titleNorm = stats.avgTitleLength() == 0 ? 1.0 : seg.titleLength(p.docId()) / stats.avgTitleLength();
                    double textNorm = stats.avgTextLength() == 0 ? 1.0 : seg.textLength(p.docId()) / stats.avgTextLength();

                    double titleComponent = TITLE_WEIGHT * p.titleTf() / (1 - TITLE_B + TITLE_B * titleNorm);
                    double textComponent = TEXT_WEIGHT * p.textTf() / (1 - TEXT_B + TEXT_B * textNorm);
                    double pseudoTf = titleComponent + textComponent;

                    if (pseudoTf == 0) continue;

                    double termScore = idf * (pseudoTf / (K1 + pseudoTf));
                    scores.merge(p.docId(), termScore, Double::sum);
                }
            }
        }
        return scores;
    }

    private List<SearchResult> topResults(Map<UUID, Double> scores,Set<String> terms, int limit){
        List<UUID> topIds = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        if (topIds.isEmpty()) return List.of();

        Map<UUID, PageMetadata> byId = pageMetadataRepo.findByIdIn(topIds).stream()
                .collect(Collectors.toMap(PageMetadata::getId, Function.identity()));

        return topIds.stream()
                .map(id -> {
                    PageMetadata page = byId.get(id);
                    if (page == null) return null; // page removed since it was indexed
                    return new SearchResult(page.getUrl(), page.getTitle(),
                            SnippetBuilder.build(page.getText(), terms), scores.get(id));
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
