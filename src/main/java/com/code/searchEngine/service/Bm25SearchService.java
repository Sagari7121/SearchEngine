package com.code.searchEngine.service;

import com.code.searchEngine.dto.SearchResult;
import com.code.searchEngine.model.DocumentIndex;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.model.Posting;
import com.code.searchEngine.repository.DocumentIndexRepo;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.repository.PostingRepo;
import com.code.searchEngine.utils.Analyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Bm25SearchService {
    private static final double K1 = 1.5;
    private static final double B = 0.75;
    private static final int MAX_CANDIDATES_PER_TERM = 2000;

    private final PostingRepo postingRepo;
    private final DocumentIndexRepo documentIndexRepo;
    private final Analyzer analyzer;
    private final PageMetadataRepo pageMetadataRepo;

    public List<SearchResult> search(String query, int limit){
        List<String> queryTerms = analyzer.tokenize(query);
        if(queryTerms.isEmpty()) return List.of();

        List<Posting> postings = queryTerms.stream()
                .flatMap(term -> postingRepo.findTopByTerm(term, MAX_CANDIDATES_PER_TERM).stream())
                .toList();
        if (postings.isEmpty()) return List.of();

        return scoreAndFetch(postings, new HashSet<>(queryTerms), limit);
    }

    public List<SearchResult> phraseSearch(String rawQuery, int limit){
        String phraseText = rawQuery.substring(1, rawQuery.length() - 1);
        List<String> phraseTerms = analyzer.tokenize(phraseText);
        if (phraseTerms.size() < 2) return search(rawQuery, limit);

        List<UUID> matchingPageIds = findPhraseMatches(phraseTerms);
        if (matchingPageIds.isEmpty()) return List.of();

        List<Posting> postings = postingRepo.findByTermInAndPageIdIn(phraseTerms, matchingPageIds);

        return scoreAndFetch(postings, new HashSet<>(phraseTerms), limit);
    }

    public List<SearchResult> scoreAndFetch(List<Posting> postings, Set<String> queryTerms, int limit){
        long totalDocs = documentIndexRepo.totalDocCount();
        if (totalDocs == 0) return List.of();
        double avgDocLength = Optional.ofNullable(documentIndexRepo.averageDocLength()).orElse(0.0);

        Map<String, Integer> docFreqByTerm = postingRepo.docFrequencies(queryTerms).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).intValue()));

        Set<UUID> pageIds = postings.stream().map(Posting::getPageId).collect(Collectors.toSet());
        Map<UUID, Integer> docLengthByPage = documentIndexRepo.findByPageIdIn(pageIds).stream()
                .collect(Collectors.toMap(DocumentIndex::getPageId, DocumentIndex::getDocLength));

        Map<UUID, Double> scores = new HashMap<>();
        for (Posting posting : postings) {
            int docFreq = docFreqByTerm.getOrDefault(posting.getTerm(), 0);
            if (docFreq == 0) continue;

            double idf = Math.log(1 + (totalDocs - docFreq + 0.5) / (docFreq + 0.5));
            int docLength = docLengthByPage.getOrDefault(posting.getPageId(), 0);
            int tf = posting.getTermFrequency();

            double numerator = tf * (K1 + 1);
            double denominator = tf + K1 * (1 - B + B * (docLength / avgDocLength));
            scores.merge(posting.getPageId(), idf * (numerator / denominator), Double::sum);
        }

        List<UUID> topIds = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        Map<UUID, PageMetadata> metadataById = pageMetadataRepo.findByIdIn(topIds).stream()
                .collect(Collectors.toMap(PageMetadata::getId, Function.identity()));

        return topIds.stream()
                .map(id -> {
                    PageMetadata page = metadataById.get(id);
                    if (page == null) return null;
                    return new SearchResult(page.getUrl(), page.getTitle(), buildSnippet(page.getText(), queryTerms), scores.get(id));
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<UUID> findPhraseMatches(List<String> phraseTerms){
        if (phraseTerms.size() < 2) return List.of();

        List<Posting> postings = postingRepo.findByTermIn(phraseTerms);

        Map<String, Map<UUID, List<Integer>>> byTermThenPage = new HashMap<>();

        for (Posting p: postings){
            List<Integer> positions = Arrays.stream(p.getPositions().split(","))
                    .map(Integer::parseInt)
                    .toList();

            byTermThenPage.computeIfAbsent(p.getTerm(), t -> new HashMap<>())
                    .put(p.getPageId(), positions);

        }

        Set<UUID> candidatePages = null;
        for(String term: phraseTerms){
            Set<UUID> pagesWithTerm = byTermThenPage.getOrDefault(term, Map.of()).keySet();
            candidatePages = candidatePages == null ? new HashSet<>(pagesWithTerm) : intersect(candidatePages, pagesWithTerm);
        }

        if (candidatePages == null || candidatePages.isEmpty()) return List.of();

        List<UUID> matches = new ArrayList<>();
        for (UUID pageId : candidatePages) {
            if (hasConsecutivePositions(phraseTerms, byTermThenPage, pageId)) {
                matches.add(pageId);
            }
        }
        return matches;
    }

    private boolean hasConsecutivePositions(List<String> terms, Map<String, Map<UUID, List<Integer>>> byTermThenPage, UUID pageId) {
        List<Integer> firstTermPositions = byTermThenPage.get(terms.get(0)).get(pageId);

        for (int startPos : firstTermPositions) {
            boolean matchesWholePhrase = true;
            for (int i = 1; i < terms.size(); i++) {
                List<Integer> nextTermPositions = byTermThenPage.get(terms.get(i)).get(pageId);
                if (nextTermPositions == null || !nextTermPositions.contains(startPos + i)) {
                    matchesWholePhrase = false;
                    break;
                }
            }
            if (matchesWholePhrase) return true; // found at least one consecutive run — that's enough
        }
        return false;
    }

    private Set<UUID> intersect(Set<UUID> a, Set<UUID> b) {
        Set<UUID> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private String buildSnippet(String text, Set<String> queryTerms) {
        if (text == null || text.isBlank()) return "";

        String[] words = text.split("\\s+");
        int hitIndex = -1;

        for (int i = 0; i < words.length; i++) {
            String cleaned = words[i].toLowerCase().replaceAll("[^a-z0-9]", "");
            if (queryTerms.contains(cleaned)) {
                hitIndex = i;
                break; // first match is fine to start — no need to find the "best" window yet
            }
        }

        if (hitIndex == -1) return String.join(" ", Arrays.asList(words).subList(0, Math.min(20, words.length)));

        int start = Math.max(0, hitIndex - 8);
        int end = Math.min(words.length, hitIndex + 12);
        return "..." + String.join(" ", Arrays.asList(words).subList(start, end)) + "...";
    }
}
