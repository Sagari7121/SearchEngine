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
@Table(name = "page_metadata", indexes = {
        @Index(columnList = "url", unique = true),
        @Index(columnList = "domainId"),
        @Index(columnList = "language"),
        @Index(columnList = "httpStatus"),
        @Index(columnList = "pageLastUpdatedAt")
})
public class PageMetadata {
    @Id
    private UUID id; // assigned in code at discovery time — see PendingCrawl

    @Column(unique = true, nullable = false)
    private String url;

    private Long domainId; // FK -> Domain.id

    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String canonicalUrl;
    private int httpStatus;
    private String language;

    private String contentHash;

    private Timestamp pageLastUpdatedAt;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageMetadata other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}