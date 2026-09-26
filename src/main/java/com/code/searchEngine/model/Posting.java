package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "posting", indexes = {
        @Index(columnList = "term"),          // the lookup you'll run constantly: "give me all docs containing X"
        @Index(columnList = "pageId"),        // needed for delete: "remove all postings for this page"
        @Index(columnList = "term, pageId", unique = true) // one row per (term, page) — matches your in-memory computeIfAbsent logic exactly
})
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String term;
    private UUID pageId; // FK -> PageMetadata.id / DocumentIndex.pageId
    private int termFrequency;
}