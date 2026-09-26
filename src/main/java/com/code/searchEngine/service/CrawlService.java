package com.code.searchEngine.service;

import com.code.searchEngine.model.Domain;
import com.code.searchEngine.model.PageLink;
import com.code.searchEngine.model.PageMetadata;
import com.code.searchEngine.model.PendingCrawl;
import com.code.searchEngine.repository.DomainRepo;
import com.code.searchEngine.repository.PageLinkRepo;
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
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final PageMetadataRepo pageMetadataRepo;
    private final PendingCrawlRepo pendingCrawlRepo;
    private final UrlBloomFilterService urlBloomFilterService;
    private final DomainRepo domainRepo;
    private final LocalSSRFChecker localSSRFChecker;
    private final UrlNormalizer urlNormalizer;
    private final PageLinkRepo pageLinkRepo;


    @Transactional
    public void crawl(PendingCrawl crawl){
        List<PageLink> newUrls = getPageData(crawl);

        if (newUrls == null) {
            retryCrawl(crawl);
        } else {
            crawlComplete(crawl, newUrls);
        }
    }

    public List<PageLink> getPageData(PendingCrawl crawl) {
        String url = crawl.getUrl();
        try{
            Document doc = request(url);

            if (doc != null) {
                String description = doc.select("meta[name=description]").attr("content");
                String text = doc.body().text();
                String title = doc.title();
                String canonicalUrl = doc.select("link[rel=canonical]").attr("abs:href");
                String language = doc.select("html").attr("lang");
                String contentHash = sha256(text);

                Timestamp now = Timestamp.from(Instant.now());
                PageMetadata metadata = PageMetadata.builder()
                        .id(crawl.getId())
                        .url(url)
                        .domainId(crawl.getDomainId())
                        .title(title)
                        .text(text)
                        .description(description)
                        .canonicalUrl(canonicalUrl)
                        .httpStatus(200)
                        .language(language)
                        .contentHash(contentHash)
                        .pageLastUpdatedAt(now)
                        .build();

                pageMetadataRepo.save(metadata);

                Map<String, String> distinctLinks = new LinkedHashMap<>();
                for (Element element : doc.select("a[href]")) {
                    String next_string = element.absUrl("href");
                    String normalizedUrl = urlNormalizer.normalize(next_string);
                    if(normalizedUrl != null && localSSRFChecker.isSafe(next_string)){
                        distinctLinks.putIfAbsent(normalizedUrl, element.text());
                    }
                }

                int MAX_PAGES_PER_CRAWL = 200;
                List<PageLink> links = new ArrayList<>();
                links = distinctLinks.entrySet().stream()
                        .limit(MAX_PAGES_PER_CRAWL)
                        .map(e -> PageLink.builder()
                                .sourcePageId(crawl.getId())
                                .targetUrl(e.getKey())
                                .targetPageId(null)
                                .anchorText(e.getValue())
                                .build())
                        .toList();


                return links.isEmpty() ? links: pageLinkRepo.saveAll(links);
            }
            return null;
        }catch (Exception e){
            if(Objects.equals(e.getMessage(), "Forbidden")){
                return new ArrayList<>();
            }
            return null;
        }
    }

    public void retryCrawl(PendingCrawl crawl){
        int attemptCount = crawl.getAttemptCount();
        Timestamp retryAfter = Timestamp.from(Instant.now().plusMillis( attemptCount * 2000L));
        crawl.setRetryAfter(retryAfter);
        crawl.setAttemptCount(crawl.getAttemptCount() + 1);
        crawl.setUpdatedAt(Timestamp.from(Instant.now()));
        pendingCrawlRepo.save(crawl);
    }


    public void crawlComplete(PendingCrawl crawl, List<PageLink> links) {
        // 1. normalize + dedupe (in-batch, then bloom filter)
        Timestamp now = new Timestamp(System.currentTimeMillis());
        pendingCrawlRepo.delete(crawl);
        if(links.isEmpty()) return;

        List<String> targetUrls = links.stream().map(PageLink::getTargetUrl).distinct().toList();
        List<String> fresh = targetUrls.stream()
                .filter(u -> !urlBloomFilterService.mightContain(u))
                .toList();

        // 2. group by host (skip malformed URLs)
        Map<String, List<String>> byHost = fresh.stream()
                .filter(u -> extractHost(u) != null)
                .collect(Collectors.groupingBy(this::extractHost));

        if (byHost.isEmpty()) {
            return;
        }

        // 3. ONE query for all domains involved
        Map<String, Domain> domainMap = domainRepo
                .getDataInDomainName(new ArrayList<>(byHost.keySet()))
                .stream()
                .collect(Collectors.toMap(Domain::getDomainName, Function.identity()));

        List<PendingCrawl> toSave = new ArrayList<>();
        List<Domain> domainsToUpdate = new ArrayList<>();
        Map<String, UUID> urlToId = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : byHost.entrySet()) {
            Domain domain = domainMap.get(entry.getKey());
            if (domain == null) {
                domain = domainRepo.save(Domain.builder()
                        .domainName(entry.getKey())
                        .nextAvailableAt(now)
                        .crawlDelayMs(500)
                        .build());
            }

            int remaining = domain.getMaxPages() - domain.getPagesCrawled();
            if (remaining <= 0) continue;

            List<String> allowed = entry.getValue().stream().limit(remaining).toList();
            if (allowed.isEmpty()) continue;

            for (String u : allowed) {
                urlBloomFilterService.add(u);
                UUID id = UUID.randomUUID();
                urlToId.put(u, id);
                toSave.add(PendingCrawl.builder()
                        .id(id)
                        .url(u)
                        .domainId(domain.getId())
                        .attemptCount(0)
                        .build());
            }

            // 5. update the count (in memory)
            domain.setPagesCrawled(domain.getPagesCrawled() + allowed.size());
            domain.setNextAvailableAt(Timestamp.from(Instant.now().plusMillis(domain.getCrawlDelayMs())));
            domainsToUpdate.add(domain);
        }

        // 6. batch writes
        if (!toSave.isEmpty()) pendingCrawlRepo.saveAll(toSave);
        if (!domainsToUpdate.isEmpty()) domainRepo.saveAll(domainsToUpdate);

        // 7. mark the current crawl as done
        links.forEach(l -> {
            UUID id = urlToId.get(l.getTargetUrl());
            if (id != null) l.setTargetPageId(id);
        });
        pageLinkRepo.saveAll(links);
    }

    private String extractHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? null : host.toLowerCase();
        } catch (IllegalArgumentException e) {
            return null; // malformed URL
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
}
