package com.code.searchEngine.service;

import com.code.searchEngine.index.segment.SegmentData;
import com.code.searchEngine.index.segment.SegmentManager;
import com.code.searchEngine.index.segment.SegmentPosting;
import com.code.searchEngine.index.segment.SegmentReader;
import com.code.searchEngine.model.DocLocation;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.repository.DocLocationRepo;
import com.code.searchEngine.utils.Analyzer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SegmentIndexer {

    private final Analyzer analyzer;
    private final SegmentManager segmentManager;
    private final DocLocationRepo docLocationRepo;

    @Transactional
    public void indexBatch(List<PageMetadata> pages) throws IOException {

        Map<String, List<SegmentPosting>> terms = new HashMap<>();
        Map<UUID, Integer> titleLengths = new HashMap<>();
        Map<UUID, Integer> textLengths = new HashMap<>();

        for (PageMetadata page : pages) {
            List<String> titleTokens = analyzer.tokenize(page.getTitle());
            List<String> textTokens = analyzer.tokenize(page.getText());
            titleLengths.put(page.getId(), titleTokens.size());
            textLengths.put(page.getId(), textTokens.size());

            Map<String, Integer> titleTf = new HashMap<>();
            for (String t : titleTokens) titleTf.merge(t, 1, Integer::sum);

            Map<String, List<Integer>> textPositions = new HashMap<>();
            for (int pos = 0; pos < textTokens.size(); pos++) {
                textPositions.computeIfAbsent(textTokens.get(pos), t -> new ArrayList<>()).add(pos);
            }

            Set<String> allTermsOnPage = new HashSet<>(titleTf.keySet());
            allTermsOnPage.addAll(textPositions.keySet());

            for (String term : allTermsOnPage) {
                int tTf = titleTf.getOrDefault(term, 0);
                List<Integer> positions = textPositions.getOrDefault(term, List.of());
                terms.computeIfAbsent(term, t -> new ArrayList<>())
                        .add(new SegmentPosting(page.getId(), tTf, positions.size(),
                                positions.stream().mapToInt(Integer::intValue).toArray()));

            }
        }

        SegmentReader written = segmentManager.flush(new SegmentData(terms, titleLengths, textLengths));
        if (written == null) return;

        List<DocLocation> locations = pages.stream()
                .map(p -> DocLocation.builder().pageId(p.getId()).segmentId(written.getSegmentId()).build())
                .toList();
        docLocationRepo.saveAll(locations);
    }
}
