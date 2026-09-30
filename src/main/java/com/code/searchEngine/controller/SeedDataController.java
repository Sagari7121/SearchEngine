package com.code.searchEngine.controller;

import com.code.searchEngine.service.SeedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedDataController {

    private final SeedDataService seedDataService;

    @PostMapping
    public ResponseEntity<String> seed() {

        seedDataService.seed();

        return ResponseEntity.ok("Seed data inserted successfully");
    }
}