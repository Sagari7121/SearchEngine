package com.code.searchEngine.index.segment;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SegmentData(
        Map<String, List<SegmentPosting>> terms,
        Map<UUID, Integer> titleLengths,
        Map<UUID, Integer> textLengths
        ) {
    public boolean isEmpty() {
        return textLengths.isEmpty();
    }
}
