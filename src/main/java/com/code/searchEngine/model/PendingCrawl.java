package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pending_crawl", indexes = {
        @Index(columnList = "url", unique = true),
        @Index(columnList = "domainId"),
        @Index(columnList = "retryAfter")
})
public class PendingCrawl {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    private String url;

    private Long domainId;

    private Timestamp retryAfter;
    private int attemptCount;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PendingCrawl other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}