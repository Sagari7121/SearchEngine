package com.code.searchEngine.repository;

import com.code.searchEngine.model.PageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageMetadataRepo extends JpaRepository<PageMetadata, Integer> {
}