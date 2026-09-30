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
@Table(name = "page_rank")
@Data
public class PageRank {
    @Id
    private UUID pageId;
    private double score;
}
