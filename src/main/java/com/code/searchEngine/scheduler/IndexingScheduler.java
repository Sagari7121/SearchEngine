package com.code.searchEngine.scheduler;

import com.code.searchEngine.model.IndexStatus;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.service.IndexingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IndexingScheduler {

    private final PageMetadataRepo pageMetadataRepo;
    private final IndexingService indexingService;

    @Transactional
    @Scheduled(fixedRate = 30_000)
    public void indexPendingPages(){
        List<PageMetadata> batch = pageMetadataRepo.findPendingForIndexing(300);

        for(PageMetadata page: batch){
            indexingService.indexDocument(page.getId(), page.getText());
            page.setIndexStatus(IndexStatus.INDEXED);
        }
        pageMetadataRepo.saveAll(batch);
    }

}
