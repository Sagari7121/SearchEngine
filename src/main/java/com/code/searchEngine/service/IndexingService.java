package com.code.searchEngine.service;

import com.code.searchEngine.model.DocumentIndex;
import com.code.searchEngine.model.Posting;
import com.code.searchEngine.repository.DocumentIndexRepo;
import com.code.searchEngine.repository.PostingRepo;
import com.code.searchEngine.utils.Analyzer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final Analyzer analyzer;
    private final DocumentIndexRepo documentIndexRepo;
    private final PostingRepo postingRepo;

    public void indexDocument(UUID pageId, String text){
        List<String> tokens = analyzer.tokenize(text);
        postingRepo.deleteByPageId(pageId);

        Map<String, Integer> termCount = new HashMap<>();
        for(String token: tokens){
            termCount.merge(token, 1, Integer::sum);
        }

        documentIndexRepo.save(DocumentIndex.builder()
                .id(UUID.randomUUID())
                .pageId(pageId)
                .docLength(tokens.size())
                .build()
        );

        List<Posting> postings = termCount.entrySet().stream()
                .map(e -> Posting.builder()
                        .term(e.getKey())
                        .pageId(pageId)
                        .termFrequency(e.getValue())
                        .build()
                ).toList();
        postingRepo.saveAll(postings);
    }
}
