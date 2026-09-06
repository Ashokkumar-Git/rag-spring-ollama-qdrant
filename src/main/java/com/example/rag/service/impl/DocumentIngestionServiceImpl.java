package com.example.rag.service.impl;

import com.example.rag.service.DocumentIngestionService;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void ingestDocuments() {
        System.out.println("Bean initialized with dependency: " + vectorStore);

        ClassPathResource resource =
                new ClassPathResource("documents/company-policy.txt");

        TextReader reader = new TextReader(resource);

        List<Document> documents = reader.get();

        TokenTextSplitter splitter =
                new TokenTextSplitter();

        List<Document> chunks =
                splitter.apply(documents);

        vectorStore.add(chunks);

        System.out.println(
                "Documents indexed: " + chunks.size()
        );
    }
}