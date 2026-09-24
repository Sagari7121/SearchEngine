package com.code.searchEngine.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Builder
public class PendingCrawl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String url;
    private int domainId;
    private Timestamp retryAfter;
    private int attemptCount;
    private Timestamp createdAt;
    private  Timestamp updatedAt;
}
