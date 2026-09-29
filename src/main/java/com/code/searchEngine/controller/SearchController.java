package com.code.searchEngine.controller;

import com.code.searchEngine.dto.SearchResponse;
import com.code.searchEngine.service.SegmentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {
    private final SegmentSearchService segmentSearchService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String rawQuery,
            @RequestParam(required = false) int limit
    ){
        if(rawQuery == null || rawQuery.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("error", "query parameter 'q' must not be empty"));
        }
        int effectiveLimit = clamp(limit);
        SearchResponse results = segmentSearchService.handleSearch(rawQuery, effectiveLimit);
        return ResponseEntity.ok(results);
    }

    private int clamp(Integer requested) {
        if (requested == null) return DEFAULT_LIMIT;
        return Math.clamp(requested, 1, MAX_LIMIT);
    }
}
