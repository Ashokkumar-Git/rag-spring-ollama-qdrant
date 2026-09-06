package com.example.rag.service;

import jakarta.annotation.PostConstruct;

public interface DocumentIngestionService {
    @PostConstruct
    void ingestDocuments();
}
