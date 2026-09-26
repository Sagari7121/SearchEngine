package com.code.searchEngine.repository;

import com.code.searchEngine.model.PageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PageLinkRepo extends JpaRepository<PageLink, Long> {
}