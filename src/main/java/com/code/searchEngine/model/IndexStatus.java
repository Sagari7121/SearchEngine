package com.code.searchEngine.model;

public enum IndexStatus {
    PENDING,   // default — not yet indexed, or content changed and needs re-indexing
    INDEXED    // successfully indexed, postings + docLength written
}