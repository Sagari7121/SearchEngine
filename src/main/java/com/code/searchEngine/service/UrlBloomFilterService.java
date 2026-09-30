package com.code.searchEngine.service;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class UrlBloomFilterService {

    private static final String FILTER_NAME = "crawled_urls_v2";

    private static final long EXPECTED_INSERTIONS = 100_000_000L;
    private static final double FALSE_POSITIVE_RATE = 0.005;

    private final RBloomFilter<String> bloomFilter;

    public UrlBloomFilterService(RedissonClient redissonClient) {

        this.bloomFilter = redissonClient.getBloomFilter(FILTER_NAME);

        if (!bloomFilter.isExists()) {

            boolean initialized = bloomFilter.tryInit(
                    EXPECTED_INSERTIONS,
                    FALSE_POSITIVE_RATE
            );

            if (!initialized) {
                throw new IllegalStateException(
                        "Failed to initialize Bloom filter"
                );
            }

            System.out.println("Bloom filter initialized");
        } else {
            System.out.println("Bloom filter already exists");
        }

        System.out.println("Bloom filter exists: " + bloomFilter.isExists());
    }

    public boolean mightContain(String url) {
        return bloomFilter.contains(url);
    }

    public void add(String url) {
        bloomFilter.add(url);
    }

    public void delete() {
        bloomFilter.delete();
    }
}