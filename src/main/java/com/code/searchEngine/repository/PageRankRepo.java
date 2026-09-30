package com.code.searchEngine.repository;

import com.code.searchEngine.model.PageRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PageRankRepo extends JpaRepository<PageRank, UUID> {
    List<PageRank> findByPageIdIn(Collection<UUID> pageIds);

    @Query("SELECT MIN(p.score) FROM PageRank p")
    Double minScore();

    @Query("SELECT MAX(p.score) FROM PageRank p")
    Double maxScore();
}
