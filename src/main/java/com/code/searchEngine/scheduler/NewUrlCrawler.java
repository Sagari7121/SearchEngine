package com.code.searchEngine.scheduler;

import com.code.searchEngine.model.PendingCrawl;
import com.code.searchEngine.repository.PendingCrawlRepo;
import com.code.searchEngine.service.CrawlService;
import com.code.searchEngine.service.UrlBloomFilterService;
import com.code.searchEngine.service.UrlNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NewUrlCrawler {

    private final PendingCrawlRepo pendingCrawlRepo;
    private final CrawlService crawlService;
    private final UrlNormalizer urlNormalizer;

    @Scheduled(cron = "0 */10 * * * *")
    public void fetchUrlsAndGetData(){
        List<PendingCrawl> crawlList = pendingCrawlRepo.getPendingUrls();
        System.out.println("CrawlList " + crawlList);

        for(PendingCrawl crawl: crawlList){
            String url = crawl.getUrl();
            ArrayList<String> newUrls = crawlService.crawl(url, crawl.getDomainId());
            crawl.setUrl(urlNormalizer.normalize(crawl.getUrl()));
            if(newUrls == null){
                crawlService.retryCrawl(crawl);
            }else
                crawlService.crawlComplete(crawl, newUrls);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
