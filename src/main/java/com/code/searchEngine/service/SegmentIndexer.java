package com.code.searchEngine.service;

import com.code.searchEngine.index.segment.SegmentData;
import com.code.searchEngine.index.segment.SegmentManager;
import com.code.searchEngine.index.segment.SegmentPosting;
import com.code.searchEngine.index.segment.SegmentReader;
import com.code.searchEngine.model.DocLocation;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.repository.DocLocationRepo;
import com.code.searchEngine.utils.Analyzer;
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

    public void indexBatch(List<PageMetadata> pages) throws IOException {

        Map<String, List<SegmentPosting>> terms = new HashMap<>();
        Map<UUID, Integer> docLengths = new HashMap<>();

        for(PageMetadata page:pages) {
            List<String> tokens = analyzer.tokenize(page.getText());
            docLengths.put(page.getId(), tokens.size());

            Map<String, List<Integer>> positionsByTerm = new HashMap<>();
            for( int pos= 0;pos<tokens.size(); pos++){
                positionsByTerm.computeIfAbsent(tokens.get(pos), t -> new ArrayList<>()).add(pos);
            }

            positionsByTerm.forEach((term, positions) ->
                    terms.computeIfAbsent(term, t -> new ArrayList<>())
                            .add(new SegmentPosting(
                                    page.getId(),
                                    positions.size(),
                                    positions.stream().mapToInt(Integer::intValue).toArray())));

        }

        SegmentReader segmentReader =  segmentManager.flush(new SegmentData(terms, docLengths));
        if(segmentReader == null) return;

        List<DocLocation> locations = pages.stream()
                .map(p -> DocLocation.builder().pageId(p.getId()).segmentId(segmentReader.getSegmentId()).build())
                .toList();

        docLocationRepo.saveAll(locations);
    }

}
