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
@Data
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
public class PageMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String url;
    private int domainId;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String text;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String canonical_url;
    private int http_status;
    private String language;
    private Timestamp created_at;
    private Timestamp updated_at;


}
