package com.code.searchEngine.scheduler;

import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.model.PendingCrawl;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.repository.PendingCrawlRepo;
import com.code.searchEngine.service.CrawlService;
import com.code.searchEngine.service.UrlBloomFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UrlCrawler {

    private final PendingCrawlRepo pendingCrawlRepo;
    private final CrawlService crawlService;
    private final PageMetadataRepo pageMetadataRepo;

    @Scheduled(cron = "0 */2 * * * *")
    public void fetchUrlsAndGetData() {
        List<PendingCrawl> crawlList = pendingCrawlRepo.getPendingUrls();

        for (PendingCrawl crawl : crawlList) {
            crawlService.crawl(crawl);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // was swallowing the interrupt — restore it instead of wrapping in RuntimeException
                return;
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void recrawlUrlsAndUpdate(){
        Timestamp cutoff = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        List<PageMetadata> due = pageMetadataRepo.findDueForRecrawl(Timestamp.from(Instant.now()), 1000);

        for (PageMetadata page : due) {
            crawlService.recrawlPage(page);
        }
    }

//    @Scheduled(fixedRate = 10000)
//    public void saveSeedData(){
//        List<PendingCrawl> pendingCrawls = pendingCrawlRepo.findAll();
//        System.out.println(pendingCrawls);
//        for(PendingCrawl crawl: pendingCrawls){
//            urlBloomFilterService.add(crawl.getUrl());
//            System.out.println(crawl.getUrl() + " " + urlBloomFilterService.mightContain(crawl.getUrl()));
//        }
//    }
}
