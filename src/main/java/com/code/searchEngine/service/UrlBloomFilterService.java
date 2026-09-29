package com.code.searchEngine.service;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class UrlBloomFilterService {
    private static final String FILTER_NAME = "crawled_urls";

    private final RBloomFilter<String> bloomFilter;



    public UrlBloomFilterService(RedissonClient redissonClient){
        this.bloomFilter = redissonClient.getBloomFilter(FILTER_NAME);


        boolean initialized =   bloomFilter.tryInit(100_000_000L, 0.005);

        System.out.println("Bloom filter initialized: " + initialized);
        System.out.println("Bloom filter exists: " + bloomFilter.isExists());

    }

    public boolean mightContain(String url){
        return bloomFilter.contains(url);
    }

    public void add(String url){
        bloomFilter.add(url);
    }
}
