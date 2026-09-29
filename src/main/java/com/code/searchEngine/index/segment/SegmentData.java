package com.code.searchEngine.index.segment;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SegmentData(
        Map<String, List<SegmentPosting>> terms,
        Map<UUID, Integer> docLengths
        ) {
    public boolean isEmpty() {
        return docLengths.isEmpty();
    }
}
