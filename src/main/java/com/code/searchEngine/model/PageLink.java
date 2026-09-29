package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "page_link", indexes = {
        @Index(columnList = "sourcePageId"),
        @Index(columnList = "targetUrl"),
        @Index(columnList = "targetPageId")
})
public class PageLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID sourcePageId;
    private String targetUrl;
    private UUID targetPageId;
    private String anchorText;

    @CreationTimestamp
    private Timestamp discoveredAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageLink other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}