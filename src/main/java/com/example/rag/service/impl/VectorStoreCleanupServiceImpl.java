package com.example.rag.service.impl;

import com.example.rag.service.VectorStoreCleanupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class VectorStoreCleanupServiceImpl implements VectorStoreCleanupService {

    private final RestClient restClient;
    private final String collectionName;

    public VectorStoreCleanupServiceImpl(
            @Value("${spring.ai.vectorstore.qdrant.collection-name}")
            String collectionName) {

        this.collectionName = collectionName;

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:6333")
                .build();
    }

    @Override
    public void deleteAllDocuments() {
        String requestBody = """
                {
                  "filter": {}
                }
                """;

        ResponseEntity<String> response = restClient
                .post()
                .uri("/collections/{collectionName}/points/delete", collectionName)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .toEntity(String.class);

        System.out.println(
                "Deleted all vectors from Qdrant collection: "
                        + collectionName
        );

        System.out.println("Qdrant response: " + response.getBody());
    }
}