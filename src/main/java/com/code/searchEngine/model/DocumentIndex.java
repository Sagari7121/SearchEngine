package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "document_index", indexes = {
        @Index(columnList = "pageId", unique = true)
})
public class DocumentIndex {

    @Id
    private UUID id;

    private UUID pageId; // same UUID as PageMetadata.id — no new id, reuse the existing one, same pattern as PageLink

    private int docLength; // total token count post-cleanup — this is what BM25's length normalization needs

    @CreationTimestamp
    private java.sql.Timestamp indexedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentIndex other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}