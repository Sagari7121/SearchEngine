package com.code.searchEngine.repository;

import com.code.searchEngine.model.DocumentIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentIndexRepo extends JpaRepository<DocumentIndex, UUID> {
    List<DocumentIndex> findByPageIdIn(Collection<UUID> pageIds);

    @Query("SELECT COUNT(d) FROM DocumentIndex d")
    long totalDocCount();

    @Query("SELECT AVG(d.docLength) FROM DocumentIndex d")
    Double averageDocLength();
}
