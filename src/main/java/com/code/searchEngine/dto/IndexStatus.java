package com.code.searchEngine.dto;

public enum IndexStatus {
    PENDING,   // default — not yet indexed, or content changed and needs re-indexing
    INDEXED    // successfully indexed, postings + docLength written
}