package com.code.searchEngine.repository;

import com.code.searchEngine.model.Crawled;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerRepo extends JpaRepository<Crawled, Integer> {
}
