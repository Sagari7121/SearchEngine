package com.code.searchEngine.controller;

import com.code.searchEngine.service.UrlBloomFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bloom")
@RequiredArgsConstructor
public class BloomController {

    private final UrlBloomFilterService bloomFilterService;

    @DeleteMapping
    public ResponseEntity<String> delete() {
        bloomFilterService.delete();
        return ResponseEntity.ok("Bloom filter deleted");
    }
}