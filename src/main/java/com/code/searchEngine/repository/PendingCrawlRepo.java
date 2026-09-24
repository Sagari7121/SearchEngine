package com.code.searchEngine.repository;

import com.code.searchEngine.model.PendingCrawl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingCrawlRepo extends JpaRepository<PendingCrawl, Integer> {

    @Query(value = """
    SELECT *
    FROM (
        SELECT
            p.*,
            ROW_NUMBER() OVER (
                PARTITION BY p.domain_id
                ORDER BY p.id
            ) AS rn
        FROM pending_crawl p
        JOIN domain d ON p.domain_id = d.id
        WHERE d.next_available_at <= CURRENT_TIMESTAMP
          AND (p.retry_after IS NULL OR p.retry_after <= CURRENT_TIMESTAMP)
          AND p.attempt_count < 3
    ) x
    WHERE x.rn <= 5
    """, nativeQuery = true)
    List<PendingCrawl> getPendingUrls();
}