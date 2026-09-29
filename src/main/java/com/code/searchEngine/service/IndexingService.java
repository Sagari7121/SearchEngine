package com.code.searchEngine.service;

import com.code.searchEngine.model.DocumentIndex;
import com.code.searchEngine.model.Posting;
import com.code.searchEngine.repository.DocumentIndexRepo;
import com.code.searchEngine.repository.PostingRepo;
import com.code.searchEngine.utils.Analyzer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final Analyzer analyzer;
    private final DocumentIndexRepo documentIndexRepo;
    private final PostingRepo postingRepo;

    public void indexDocument(UUID pageId, String text){
        List<String> tokens = analyzer.tokenize(text);
        postingRepo.deleteByPageId(pageId);

        Map<String, List<Integer>> termPositions = new HashMap<>();
        for (int position = 0; position < tokens.size(); position++) {
            termPositions.computeIfAbsent(tokens.get(position), t -> new ArrayList<>()).add(position);
        }

        List<Posting> postings = termPositions.entrySet().stream()
                .map(e -> Posting.builder()
                        .term(e.getKey())
                        .pageId(pageId)
                        .termFrequency(e.getValue().size())
                        .positions(e.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")))
                        .build())
                .toList();
        postingRepo.saveAll(postings);

        DocumentIndex docIndex = documentIndexRepo.findByPageId(pageId)
                .orElseGet(() -> DocumentIndex.builder().id(UUID.randomUUID()).pageId(pageId).build());
        docIndex.setDocLength(tokens.size());
        documentIndexRepo.save(docIndex);
    }
}
