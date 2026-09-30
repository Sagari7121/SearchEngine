package com.code.searchEngine.service;

import com.code.searchEngine.robots.RobotRules;
import com.code.searchEngine.robots.RobotsTxtParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RobotsTxtService {

    public static final String USER_AGENT = "CodeSearchBot";
    private static final long CACHE_TTL_SECONDS = TimeUnit.HOURS.toSeconds(24);

    private record CachedRules(RobotRules rules, Instant fetchedAt){
        boolean isExpired(){
            return Instant.now().isAfter(fetchedAt.plusSeconds(CACHE_TTL_SECONDS));
        }
    }

    private final ConcurrentHashMap<String, CachedRules> cache = new ConcurrentHashMap<>();

    public RobotRules getRules(String host){
        CachedRules cached = cache.get(host);

        if(cached != null && !cached.isExpired()) return cached.rules;

        RobotRules rules = fetch(host);
        cache.put(host, new CachedRules(rules, Instant.now()));
        return rules;
    }

    private RobotRules fetch(String host){
        String robotsUrl = "https://" + host + "/robots.txt";
        try{
            Connection.Response response = Jsoup.connect(robotsUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10_000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();
            if(status == 404){
                return RobotRules.allowAll();
            }
            if(status>=400){
                log.warn("robots.txt fetch for {} returned {}, treating as allow-all", host, status);
                return RobotRules.allowAll();
            }
            return RobotsTxtParser.parse(response.body(), USER_AGENT);
        }catch (Exception e){
            log.warn("robots.txt fetch failed for {}: {} — treating as allow-all", host, e.getMessage());
            return RobotRules.allowAll();
        }
    }

}
