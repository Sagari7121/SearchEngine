package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "domain", indexes = {
        @Index(columnList = "domainName", unique = true),
        @Index(columnList = "nextAvailableAt")
})
public class Domain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    private String domainName;

    private Timestamp nextAvailableAt;
    private int crawlDelayMs;

    @Builder.Default
    @Column(nullable = false)
    private int maxPages = 5_000;

    @Builder.Default
    @Column(nullable = false)
    private int pagesCrawled = 0;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Domain other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}