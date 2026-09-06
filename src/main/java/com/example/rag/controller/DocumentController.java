package com.example.rag.controller;

import com.example.rag.service.DocumentIngestionService;
import com.example.rag.service.VectorStoreCleanupService;
import com.example.rag.service.impl.VectorStoreCleanupServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final VectorStoreCleanupService cleanupService;
    private final DocumentIngestionService ingestionService;

    public DocumentController(
            VectorStoreCleanupServiceImpl cleanupService, DocumentIngestionService ingestionService) {
        this.cleanupService = cleanupService;
        this.ingestionService = ingestionService;
    }

    @DeleteMapping
    public ResponseEntity<String> deleteAll() {

        cleanupService.deleteAllDocuments();

        return ResponseEntity.ok(
                "All documents deleted successfully"
        );
    }

    @PutMapping
    public ResponseEntity<String> ingestDocuments() {

        ingestionService.ingestDocuments();

        return ResponseEntity.ok(
                "Documents ingested successfully"
        );
    }
}