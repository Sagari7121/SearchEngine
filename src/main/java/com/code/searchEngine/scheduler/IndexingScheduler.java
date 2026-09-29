package com.code.searchEngine.scheduler;

import com.code.searchEngine.dto.IndexStatus;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.service.IndexingService;
import com.code.searchEngine.service.SegmentIndexer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IndexingScheduler {

    private final PageMetadataRepo pageMetadataRepo;
    private final SegmentIndexer segmentIndexer;

    @Transactional
    @Scheduled(fixedRate = 30_000)
    public void indexPendingPages() throws IOException {
        List<PageMetadata> batch = pageMetadataRepo.findPendingForIndexing(500);
        if(batch.isEmpty()) return;

        segmentIndexer.indexBatch(batch);

        batch.forEach(p -> p.setIndexStatus(IndexStatus.INDEXED));
        pageMetadataRepo.saveAll(batch);
    }

}
