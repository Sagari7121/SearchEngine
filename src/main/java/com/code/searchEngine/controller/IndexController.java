package com.code.searchEngine.controller;

import com.code.searchEngine.service.IndexResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class IndexController {

    private final IndexResetService indexResetService;

    @PostMapping("/resetIndexing")
    public ResponseEntity<?> resetIndexing(){
        try {
            indexResetService.resetIndex();
            return ResponseEntity.ok("Successfully reset");
        }catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
