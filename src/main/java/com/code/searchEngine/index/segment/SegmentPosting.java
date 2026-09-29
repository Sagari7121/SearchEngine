package com.code.searchEngine.index.segment;

import java.util.UUID;

public record SegmentPosting(UUID docId, int termFrequency, int[] positions) {}