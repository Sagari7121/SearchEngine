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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
                int MAX_PAGES_PER_CRAWL = 200;
                return new ArrayList<>(newUrls.subList(0, Math.min(newUrls.size(), MAX_PAGES_PER_CRAWL)));
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
    public void crawlComplete(PendingCrawl crawl, List<String> newUrls) {
        // 1. normalize + dedupe (in-batch, then bloom filter)
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<String> fresh = newUrls.stream()
                .map(urlNormalizer::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .filter(u -> !urlBloomFilterService.mightContain(u))
                .toList();

        // 2. group by host (skip malformed URLs)
        Map<String, List<String>> byHost = fresh.stream()
                .filter(u -> extractHost(u) != null)
                .collect(Collectors.groupingBy(this::extractHost));

        if (byHost.isEmpty()) {
            pendingCrawlRepo.delete(crawl);
            crawlerRepo.save(Crawled.builder()
                    .url(crawl.getUrl()).domainId(crawl.getDomainId()).createdAt(now).updatedAt(now).build());
            return;
        }

        // 3. ONE query for all domains involved
        Map<String, Domain> domainMap = domainRepo
                .getDataInDomainName(new ArrayList<>(byHost.keySet()))
                .stream()
                .collect(Collectors.toMap(Domain::getDomainName, Function.identity()));

        List<PendingCrawl> toSave = new ArrayList<>();
        List<Domain> domainsToUpdate = new ArrayList<>();


        for (Map.Entry<String, List<String>> entry : byHost.entrySet()) {
            Domain domain = domainMap.get(entry.getKey());
            if (domain == null) {
                domain = Domain.builder()
                        .domainName(entry.getKey())
                        .nextAvailableAt(now)
                        .crawlDelayMs(500)
                        .createAt(now)
                        .updatedAt(now)
                        .build();
            // untracked domain: skip (or create it)
            }

            int remaining = domain.getMax_pages() - domain.getPages_crawled();
            if (remaining <= 0) continue; // limit reached, drop all

            // 4. drop the extras
            List<String> allowed = entry.getValue().stream()
                    .limit(remaining)
                    .toList();
            if (allowed.isEmpty()) continue;

            for (String u : allowed) {
                urlBloomFilterService.add(u);
                toSave.add(PendingCrawl.builder()
                        .url(u)
                        .domainId(domain.getId())
                                .retryAfter(null)
                                .attemptCount(0)
                                .createdAt(now)
                                .updatedAt(now)
                        .build());
            }

            // 5. update the count (in memory)
            domain.setPages_crawled(domain.getPages_crawled() + allowed.size());
            domain.setUpdatedAt(now);
            domain.setNextAvailableAt(Timestamp.from(Instant.now().plusMillis(5000)));
            domainsToUpdate.add(domain);
        }

        // 6. batch writes
        if (!toSave.isEmpty()) pendingCrawlRepo.saveAll(toSave);
        if (!domainsToUpdate.isEmpty()) domainRepo.saveAll(domainsToUpdate);

        // 7. mark the current crawl as done
        pendingCrawlRepo.delete(crawl);
        crawlerRepo.save(Crawled.builder()
                .url(crawl.getUrl()).domainId(crawl.getDomainId()).createdAt(now).updatedAt(now).build());
    }

    private String extractHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? null : host.toLowerCase();
        } catch (IllegalArgumentException e) {
            return null; // malformed URL
        }
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
