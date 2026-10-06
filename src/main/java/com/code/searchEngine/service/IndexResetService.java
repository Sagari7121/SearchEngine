package com.code.searchEngine.service;

import com.code.searchEngine.index.segment.SegmentManager;
import com.code.searchEngine.repository.DocLocationRepo;
import com.code.searchEngine.repository.PageMetadataRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class IndexResetService {

    private final DocLocationRepo docLocationRepo;
    private final PageMetadataRepo pageMetadataRepo;
    private final SegmentManager segmentManager;

    @Transactional
    public void resetIndex() throws IOException {
        segmentManager.resetAll();
        docLocationRepo.deleteAll();
        pageMetadataRepo.markAllPending();
    }
}