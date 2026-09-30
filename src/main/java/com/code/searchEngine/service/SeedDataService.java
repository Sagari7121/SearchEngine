package com.code.searchEngine.service;

import com.code.searchEngine.model.Domain;
import com.code.searchEngine.model.PendingCrawl;
import com.code.searchEngine.repository.DomainRepo;
import com.code.searchEngine.repository.PendingCrawlRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SeedDataService {

    private final DomainRepo domainRepository;
    private final PendingCrawlRepo pendingCrawlRepository;
    private final UrlBloomFilterService urlBloomFilterService;

    @Transactional
    public void seed() {

        /*
         * domain -> URLs
         */
        Map<String, List<String>> seedData = new LinkedHashMap<>();

        // ---------------- CODING ----------------

        seedData.put("developer.mozilla.org", List.of(
                "https://developer.mozilla.org/en-US/docs/Web/JavaScript",
                "https://developer.mozilla.org/en-US/docs/Web/HTTP",
                "https://developer.mozilla.org/en-US/docs/Web/API",
                "https://developer.mozilla.org/en-US/docs/Learn",
                "https://developer.mozilla.org/en-US/docs/Web/CSS"
        ));

        seedData.put("docs.python.org", List.of(
                "https://docs.python.org/3/tutorial/",
                "https://docs.python.org/3/library/",
                "https://docs.python.org/3/reference/",
                "https://docs.python.org/3/howto/",
                "https://docs.python.org/3/faq/"
        ));

        seedData.put("spring.io", List.of(
                "https://spring.io/projects/spring-boot",
                "https://spring.io/projects/spring-framework",
                "https://spring.io/guides",
                "https://spring.io/blog",
                "https://spring.io/projects/spring-data"
        ));

        // ---------------- TECHNOLOGY ----------------

        seedData.put("aws.amazon.com", List.of(
                "https://aws.amazon.com/ec2/",
                "https://aws.amazon.com/s3/",
                "https://aws.amazon.com/lambda/",
                "https://aws.amazon.com/rds/",
                "https://aws.amazon.com/dynamodb/"
        ));

        seedData.put("kafka.apache.org", List.of(
                "https://kafka.apache.org/documentation/",
                "https://kafka.apache.org/quickstart",
                "https://kafka.apache.org/intro",
                "https://kafka.apache.org/uses",
                "https://kafka.apache.org/07/quickstart"
        ));

        // ---------------- MEDICINE / HEALTH ----------------

        seedData.put("who.int", List.of(
                "https://www.who.int/health-topics/diabetes",
                "https://www.who.int/health-topics/cancer",
                "https://www.who.int/health-topics/mental-health",
                "https://www.who.int/health-topics/nutrition",
                "https://www.who.int/health-topics/vaccines-and-immunization"
        ));

        seedData.put("mayoclinic.org", List.of(
                "https://www.mayoclinic.org/diseases-conditions/diabetes",
                "https://www.mayoclinic.org/diseases-conditions/high-blood-pressure",
                "https://www.mayoclinic.org/diseases-conditions/asthma",
                "https://www.mayoclinic.org/diseases-conditions/migraine-headache",
                "https://www.mayoclinic.org/healthy-lifestyle/nutrition-and-healthy-eating"
        ));

        // ---------------- AGRICULTURE ----------------

        seedData.put("fao.org", List.of(
                "https://www.fao.org/land-water/en/",
                "https://www.fao.org/agriculture/en/",
                "https://www.fao.org/fishery/en/",
                "https://www.fao.org/forestry/en/",
                "https://www.fao.org/climate-change/en/"
        ));

        seedData.put("icar.gov.in", List.of(
                "https://icar.gov.in/",
                "https://icar.gov.in/content/agricultural-research",
                "https://icar.gov.in/content/education",
                "https://icar.gov.in/content/extension",
                "https://icar.gov.in/content/publications"
        ));

        // ---------------- FOOD ----------------

        seedData.put("bbcgoodfood.com", List.of(
                "https://www.bbcgoodfood.com/recipes",
                "https://www.bbcgoodfood.com/recipes/collection/healthy-recipes",
                "https://www.bbcgoodfood.com/recipes/collection/vegetarian-recipes",
                "https://www.bbcgoodfood.com/recipes/collection/chicken-recipes",
                "https://www.bbcgoodfood.com/recipes/collection/dessert-recipes"
        ));

        seedData.put("allrecipes.com", List.of(
                "https://www.allrecipes.com/recipes/",
                "https://www.allrecipes.com/recipes/17562/dinner/",
                "https://www.allrecipes.com/recipes/78/breakfast-and-brunch/",
                "https://www.allrecipes.com/recipes/79/desserts/",
                "https://www.allrecipes.com/recipes/85/world-cuisine/"
        ));

        // ---------------- FASHION ----------------

        seedData.put("vogue.com", List.of(
                "https://www.vogue.com/fashion",
                "https://www.vogue.com/fashion-shows",
                "https://www.vogue.com/fashion/street-style",
                "https://www.vogue.com/fashion/trends",
                "https://www.vogue.com/fashion/sustainable-fashion"
        ));

        seedData.put("gq.com", List.of(
                "https://www.gq.com/style",
                "https://www.gq.com/fashion",
                "https://www.gq.com/watches",
                "https://www.gq.com/grooming",
                "https://www.gq.com/lifestyle"
        ));

        // ---------------- SCIENCE ----------------

        seedData.put("nasa.gov", List.of(
                "https://www.nasa.gov/missions/",
                "https://www.nasa.gov/solar-system/",
                "https://www.nasa.gov/earth/",
                "https://www.nasa.gov/humans-in-space/",
                "https://www.nasa.gov/space-science/"
        ));

        seedData.put("nature.com", List.of(
                "https://www.nature.com/subjects/biology",
                "https://www.nature.com/subjects/physics",
                "https://www.nature.com/subjects/chemistry",
                "https://www.nature.com/subjects/neuroscience",
                "https://www.nature.com/subjects/genetics"
        ));

        // ---------------- FINANCE ----------------

        seedData.put("investopedia.com", List.of(
                "https://www.investopedia.com/investing-4427685",
                "https://www.investopedia.com/personal-finance-4427760",
                "https://www.investopedia.com/mortgages-4689761",
                "https://www.investopedia.com/insurance-4427712",
                "https://www.investopedia.com/retirement-4427686"
        ));

        // ---------------- TRAVEL ----------------

        seedData.put("lonelyplanet.com", List.of(
                "https://www.lonelyplanet.com/",
                "https://www.lonelyplanet.com/india",
                "https://www.lonelyplanet.com/asia",
                "https://www.lonelyplanet.com/europe",
                "https://www.lonelyplanet.com/africa"
        ));

        // ---------------- SPORTS ----------------

        seedData.put("espn.com", List.of(
                "https://www.espn.com/nfl/",
                "https://www.espn.com/nba/",
                "https://www.espn.com/mlb/",
                "https://www.espn.com/soccer/",
                "https://www.espn.com/tennis/"
        ));

        // ---------------- EDUCATION ----------------

        seedData.put("khanacademy.org", List.of(
                "https://www.khanacademy.org/math",
                "https://www.khanacademy.org/science",
                "https://www.khanacademy.org/computing",
                "https://www.khanacademy.org/economics-finance-domain",
                "https://www.khanacademy.org/humanities"
        ));

        // ---------------- HISTORY ----------------

        seedData.put("britannica.com", List.of(
                "https://www.britannica.com/topic/World-War-II",
                "https://www.britannica.com/topic/Industrial-Revolution",
                "https://www.britannica.com/event/French-Revolution",
                "https://www.britannica.com/topic/Roman-Empire",
                "https://www.britannica.com/topic/Indian-history"
        ));

        // ---------------- ENVIRONMENT ----------------

        seedData.put("nationalgeographic.com", List.of(
                "https://www.nationalgeographic.com/environment",
                "https://www.nationalgeographic.com/animals",
                "https://www.nationalgeographic.com/science",
                "https://www.nationalgeographic.com/travel",
                "https://www.nationalgeographic.com/history"
        ));

        // ============================================================
        // CREATE DOMAINS
        // ============================================================

        List<Domain> domains = new ArrayList<>();

        for (String domainName : seedData.keySet()) {

            Domain domain = Domain.builder()
                    .domainName(domainName)
                    .nextAvailableAt(Timestamp.from(Instant.now()))
                    .crawlDelayMs(1000)
                    .maxPages(5_000)
                    .pagesCrawled(0)
                    .build();

            domains.add(domain);
        }

        domainRepository.saveAll(domains);

        // ============================================================
        // CREATE PENDING CRAWLS
        // ============================================================

        List<PendingCrawl> pendingCrawls = new ArrayList<>();

        for (Domain domain : domains) {

            List<String> urls = seedData.get(domain.getDomainName());

            for (String url : urls) {

                PendingCrawl pendingCrawl = PendingCrawl.builder()
                        .id(UUID.randomUUID())
                        .url(url)
                        .domainId(domain.getId())
                        .retryAfter(null)
                        .attemptCount(0)
                        .lastError(null)
                        .build();

                pendingCrawls.add(pendingCrawl);
            }
        }

        pendingCrawlRepository.saveAll(pendingCrawls);
    }
}