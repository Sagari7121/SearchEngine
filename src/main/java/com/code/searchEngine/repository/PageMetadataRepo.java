package com.code.searchEngine.repository;

import com.code.searchEngine.model.PageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PageMetadataRepo extends JpaRepository<PageMetadata, UUID> {

    @Query(value = """
    SELECT * FROM page_metadata
    WHERE index_status = 'PENDING'
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<PageMetadata> findPendingForIndexing(@Param("limit") int limit);


    List<PageMetadata> findByIdIn(Collection<UUID> ids);

    @Query(value = """
    SELECT * FROM page_metadata
    WHERE updated_at <= :cutoff
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<PageMetadata> findDueForRecrawl(@Param("cutoff") Timestamp cutoff, @Param("limit") int limit);
}