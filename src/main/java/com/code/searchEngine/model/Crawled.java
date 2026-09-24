package com.code.searchEngine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;


@NoArgsConstructor
@AllArgsConstructor
@Component
@Entity
@Builder
public class Crawled {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(columnDefinition = "TEXT")
    private String url;
    private int domainId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
