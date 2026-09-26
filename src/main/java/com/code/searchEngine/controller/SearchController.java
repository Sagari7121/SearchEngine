package com.code.searchEngine.controller;

import com.code.searchEngine.dto.SearchResponse;
import com.code.searchEngine.dto.SearchResult;
import com.code.searchEngine.service.Bm25SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {
    private final Bm25SearchService bm25SearchService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) int limit
    ){
        if(q == null || q.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("error", "query parameter 'q' must not be empty"));
        }
        int effectiveLimit = clamp(limit);

        long start = System.currentTimeMillis();
        List<SearchResult> results = bm25SearchService.search(q, effectiveLimit);
        long took = System.currentTimeMillis() - start;

        return ResponseEntity.ok(new SearchResponse(q, results.size(), took, results));
    }

    private int clamp(Integer requested) {
        if (requested == null) return DEFAULT_LIMIT;
        return Math.clamp(requested, 1, MAX_LIMIT);
    }
}
