package com.code.searchEngine.repository;

import com.code.searchEngine.model.DocLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocLocationRepo extends JpaRepository<DocLocation, UUID> {

    List<DocLocation> findByPageIdIn(Collection<UUID> pageIds);
    void deleteByPageIdIn(Collection<UUID> pageIds);
}
