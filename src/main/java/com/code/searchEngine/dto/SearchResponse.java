package com.code.searchEngine.dto;

import java.util.List;

public record SearchResponse(
        String query,
        int resultCount,
        long tookMillis,
        List<SearchResult> results
) {}