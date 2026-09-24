package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@Builder
public class Domain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String domainName;
    private Timestamp nextAvailableAt;
    private int crawlDelayMs;

    @Builder.Default
    @Column(nullable = false)
    private int max_pages = 5_000;

    @Builder.Default
    @Column(nullable = false)
    private int pages_crawled = 0;
    private Timestamp createAt;
    private Timestamp updatedAt;
}
