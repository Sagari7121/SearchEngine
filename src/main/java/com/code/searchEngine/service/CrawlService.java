package com.code.searchEngine.service;

import com.code.searchEngine.model.Crawled;
import com.code.searchEngine.model.Domain;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.model.PendingCrawl;
import com.code.searchEngine.repository.CrawlerRepo;
import com.code.searchEngine.repository.DomainRepo;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.repository.PendingCrawlRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final PageMetadataRepo pageMetadataRepo;
    private final PendingCrawlRepo pendingCrawlRepo;
    private final CrawlerRepo crawlerRepo;
    private final UrlBloomFilterService urlBloomFilterService;
    private final DomainRepo domainRepo;
    private final LocalSSRFChecker localSSRFChecker;
    private final UrlNormalizer urlNormalizer;
    private final int MAX_PAGES_PER_CRAWL = 200;


    public ArrayList<String> crawl(String url, int domainId) {
        try{
            Document doc = request(url);
            ArrayList<String> newUrls = new ArrayList<>();

            if (doc != null) {
                String description = doc.select("meta[name=description]")
                        .attr("content");
                String text = doc.body().text();
                String title = doc.title();
                String canonicalUrl = doc.select("link[rel=canonical]").attr("abs:href");
                String language = doc.select("html").attr("lang");

                Timestamp now = Timestamp.from(Instant.now());
                PageMetadata metadata = PageMetadata.builder()
                        .url(url)
                        .domainId(domainId)
                        .title(title)
                        .text(text)
                        .description(description)
                        .canonical_url(canonicalUrl)
                        .http_status(200)
                        .language(language)
                        .created_at(now)
                        .updated_at(now)
                        .build();

                pageMetadataRepo.save(metadata);

                for (Element element : doc.select("a[href]")) {
                    String next_string = element.absUrl("href");
                    String normalizedUrl = urlNormalizer.normalize(next_string);
                    if(normalizedUrl != null && localSSRFChecker.isSafe(next_string)){
                        newUrls.add(normalizedUrl);
                    }
                }
                return new ArrayList<>(newUrls.subList(0, Math.min(newUrls.size(), 500)));
            }
            return null;
        }catch (Exception e){
            if(Objects.equals(e.getMessage(), "Forbidden")){
                return new ArrayList<>();
            }
            return null;
        }
    }

    @Transactional
    public void retryCrawl(PendingCrawl crawl){
        int attemptCount = crawl.getAttemptCount();
        Timestamp retryAfter = Timestamp.from(Instant.now().plusMillis( attemptCount * 2000L));
        crawl.setRetryAfter(retryAfter);
        crawl.setAttemptCount(crawl.getAttemptCount() + 1);
        crawl.setUpdatedAt(Timestamp.from(Instant.now()));
        pendingCrawlRepo.save(crawl);
    }

    @Transactional
    public void crawlComplete(PendingCrawl completedCrawl, List<String> newUrls) {
        Timestamp now = Timestamp.from(Instant.now());
        pendingCrawlRepo.delete(completedCrawl);
        crawlerRepo.save(
                Crawled.builder()
                        .url(completedCrawl.getUrl())
                        .domainId(completedCrawl.getDomainId())
                        .createdAt(now)
                        .updatedAt(now)
                        .build()

        );
        if(newUrls==null){
            return;
        }
        List<String> filteredUrls = newUrls.stream()
                .filter(url -> !urlBloomFilterService.mightContain(url))
                .toList();

        if (filteredUrls.isEmpty()) {
            return;
        }
        Set<String> domainNames = filteredUrls.stream()
                .map(this::getDomain)
                .collect(Collectors.toSet());
        System.out.println("DomainNames: " + domainNames);
        List<Domain> domains = domainRepo.getDataInDomainName(
                domainNames.stream().toList()
        );

        Set<String> existingDomainNames = domains.stream()
                .map(Domain::getDomainName)
                .collect(Collectors.toSet());
        List<Domain> newDomains = domainNames.stream()
                .filter(domainName -> !existingDomainNames.contains(domainName))
                .map(domainName ->  Domain.builder().domainName(
                        domainName

                ).nextAvailableAt(now).crawlDelayMs(500).createAt(now).updatedAt(now).build())
                .toList();
        List<Domain> savedNewDomains = domainRepo.saveAll(newDomains);
        domains.addAll(savedNewDomains);
        Map<String, Integer> domainIdMap = domains.stream()
                .collect(Collectors.toMap(
                        Domain::getDomainName,
                        Domain::getId
                ));

        List<PendingCrawl> pendingCrawls = filteredUrls.stream()
                .map(url ->  PendingCrawl.builder().url(url)
                        .domainId(domainIdMap.get(getDomain(url)))
                        .retryAfter(null)
                        .attemptCount(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
                )
                .toList();
        pendingCrawlRepo.saveAll(pendingCrawls);

        Timestamp nextAvailableAt = Timestamp.from(
                Instant.now().plusMillis(1000)
        );
        domains.stream()
                .filter(domain -> existingDomainNames.contains(domain.getDomainName()))
                .forEach(domain -> domain.setNextAvailableAt(nextAvailableAt));

        domainRepo.saveAll(
                domains.stream()
                        .filter(domain -> existingDomainNames.contains(domain.getDomainName()))
                        .toList()
        );
    }

    private static Document request(String url) {
        try {
            Connection con = Jsoup.connect(url)
                    .timeout(10_000)
                    .maxBodySize(2 * 1024 * 1024)
                    .ignoreContentType(true);

            Document doc = con.get();

            if (con.response().statusCode() == 200) {
                String contentType = con.response().contentType();
                if (contentType == null || !contentType.startsWith("text/html")) return null;

                System.out.println("Link: " + url);
                System.out.println(doc.title());
                return doc;
            } else if(con.response().statusCode() == 403){
                throw new RuntimeException("Forbidden");
            }
            return null;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private String getDomain(String url) {
        URI uri = URI.create(url);
        return uri.getHost();
    }
}
