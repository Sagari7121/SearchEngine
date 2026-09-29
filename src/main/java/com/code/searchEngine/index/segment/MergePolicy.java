package com.code.searchEngine.index.segment;

import com.code.searchEngine.model.DocLocation;
import com.code.searchEngine.repository.DocLocationRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MergePolicy {

    private static final int MERGE_TRIGGER_COUNT = 10;
    private final SegmentManager segmentManager;
    private final DocLocationRepo docLocationRepo;

    public MergePolicy(SegmentManager segmentManager, DocLocationRepo docLocationRepo) {
        this.segmentManager = segmentManager;
        this.docLocationRepo = docLocationRepo;
    }

    @Scheduled(fixedRate = 60_000)
    public void maybeMerge() {
        List<SegmentReader> segments = segmentManager.getActiveSegments();
        if (segments.size() < MERGE_TRIGGER_COUNT) return;

        try {
            mergeAll(segments);
        } catch (IOException e) {
            log.error("Merge failed, originals left untouched", e);
        }
    }

    private void mergeAll(List<SegmentReader> inputs) throws IOException {
        Set<UUID> allDocIdsInInputs = new HashSet<>();
        for (SegmentReader seg : inputs) allDocIdsInInputs.addAll(seg.allDocIds());

        Map<UUID, Long> liveSegmentByDoc = docLocationRepo.findByPageIdIn(allDocIdsInInputs)
                .stream().collect(Collectors.toMap(DocLocation::getPageId, DocLocation::getSegmentId));

        Map<String, List<SegmentPosting>> terms = new HashMap<>();
        Map<UUID, Integer> titleLengths = new HashMap<>();
        Map<UUID, Integer> textLengths = new HashMap<>();

        for (SegmentReader seg : inputs) {
            for (UUID docId : seg.allDocIds()) {
                Long liveSegment = liveSegmentByDoc.get(docId);
                if (liveSegment == null || !liveSegment.equals(seg.getSegmentId())) continue;
                titleLengths.put(docId, seg.titleLength(docId));
                textLengths.put(docId, seg.textLength(docId));
            }
            for (String term : seg.allTerms()) {
                for (SegmentPosting p : seg.getPostings(term)) {
                    Long liveSegment = liveSegmentByDoc.get(p.docId());
                    if (liveSegment == null || !liveSegment.equals(seg.getSegmentId())) continue;
                    terms.computeIfAbsent(term, t -> new ArrayList<>()).add(p);
                }
            }
        }

        SegmentReader merged = segmentManager.replaceWithMerged(inputs, new SegmentData(terms, titleLengths, textLengths));

        List<DocLocation> updated = textLengths.keySet().stream()
                .map(id -> DocLocation.builder().pageId(id).segmentId(merged.getSegmentId()).build())
                .toList();
        docLocationRepo.saveAll(updated);

        log.info("Merged {} input segments -> segment_{}, kept {} live docs, dropped {} stale/deleted",
                inputs.size(), merged.getSegmentId(), textLengths.size(), allDocIdsInInputs.size() - textLengths.size());
    }
}