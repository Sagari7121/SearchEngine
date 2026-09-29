package com.code.searchEngine.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "doc_location")
public class DocLocation {
    @Id
    public UUID pageId;
    public long segmentId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocLocation other)) return false;
        return pageId != null && pageId.equals(other.pageId);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
