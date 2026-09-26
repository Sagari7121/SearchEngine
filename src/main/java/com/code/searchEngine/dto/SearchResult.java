package com.code.searchEngine.dto;

public record SearchResult(
        String url,
        String title,
        String snippet,
        double score
) {}