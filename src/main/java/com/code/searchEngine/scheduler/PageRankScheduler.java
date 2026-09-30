package com.code.searchEngine.scheduler;

import com.code.searchEngine.model.PageLink;
import com.code.searchEngine.model.PageRank;
import com.code.searchEngine.repository.PageLinkRepo;
import com.code.searchEngine.repository.PageMetadataRepo;
import com.code.searchEngine.repository.PageRankRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageRankScheduler {

    private static final double DAMPING = 0.85;
    private static final int MAX_ITERATIONS = 50;
    private static final double CONVERGENCE_THRESHOLD = 1e-6;

    private final PageMetadataRepo pageMetadataRepo;
    private final PageLinkRepo pageLinkRepo;
    private final PageRankRepo pageRankRepo;

    @Scheduled(cron = "0 0 3 * * *")
    public void recomputePageRanks() {
        List<UUID> allPageIds = pageMetadataRepo.findAll().stream()
                .map(page -> page.getId()).toList();

        if(allPageIds.isEmpty()) return;

        Map<UUID, Set<UUID>> outboundLinks = new HashMap<>();
        for(PageLink link: pageLinkRepo.findAll()){
            if(link.getTargetPageId() == null) continue;
            outboundLinks.computeIfAbsent(link.getSourcePageId(), t -> new HashSet<>())
                    .add(link.getTargetPageId());
        }

        int n = allPageIds.size();
        Map<UUID, Double> scores = new HashMap<>();

        for (UUID id : allPageIds) scores.put(id, 1.0 / n);

        for(int i=0;i<MAX_ITERATIONS;i++){
            Map<UUID, Double> next = new HashMap<>();
            for (UUID id : allPageIds) next.put(id, 1 - DAMPING);

            for(UUID source: allPageIds){
                Set<UUID> targets = outboundLinks.getOrDefault(source, Set.of());
                if (targets.isEmpty()) continue;
                double share = DAMPING * scores.get(source) / targets.size();
                for (UUID target : targets) {
                    next.merge(target, share, Double::sum);
                }
            }
            double delta = 0.0;
            for (UUID id : allPageIds) delta += Math.abs(next.get(id) - scores.get(id));

            scores = next;
            if (delta < CONVERGENCE_THRESHOLD) {
                log.info("PageRank converged after {} iterations", i + 1);
                break;
            }
        }

        List<PageRank> toSave = scores.entrySet().stream()
                .map(e -> PageRank.builder().pageId(e.getKey()).score(e.getValue()).build())
                .toList();
        pageRankRepo.saveAll(toSave);
        log.info("PageRank recomputed for {} pages", toSave.size());
    }

}
