package com.code.searchEngine.index.segment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class MergePolicy {

    private static final int MERGE_TRIGGER_COUNT = 10;
    private final SegmentManager segmentManager;

    public MergePolicy(SegmentManager segmentManager) {
        this.segmentManager = segmentManager;
    }

    @Scheduled(fixedRate = 60_000)
    public void maybeMerge(){
        List<SegmentReader> segments = segmentManager.getActiveSegments();
        if (segments.size() < MERGE_TRIGGER_COUNT) return;

        try{
            mergeAll(segments);
        }catch (IOException e){
            log.error("Merge failed, originals left untouched", e);
        }
    }

    private void mergeAll(List<SegmentReader> inputs) throws IOException{
        Map<String, List<SegmentPosting>> terms = new HashMap<>();
        Map<UUID, Integer> docLengths = new HashMap<>();

        for(SegmentReader seg: inputs){
            for(UUID docId: seg.allDocIds()){
                docLengths.put(docId, seg.docLength(docId));
            }
            for(String term: seg.allTerms()){
                terms.computeIfAbsent(term, t -> new ArrayList<>()).addAll(seg.getPostings(term));
            }
        }

        SegmentReader mergedReader = segmentManager.replaceWithMerged(inputs, new SegmentData(terms, docLengths));
        log.info("Merged {} segments into segment_{}", inputs.size(), mergedReader.getSegmentId());

    }

}
