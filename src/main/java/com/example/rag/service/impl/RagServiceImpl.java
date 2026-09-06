package com.example.rag.service.impl;

import com.example.rag.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagServiceImpl(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore) {

        this.chatClient =
                chatClientBuilder.build();

        this.vectorStore = vectorStore;
    }

    @Override
    public String ask(String question) {

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(5)
                                .build()
                );

        String context = documents.stream()
                .map(Document::getText)
                .reduce(
                        "",
                        (a, b) -> a + "\n\n" + b
                );
        String prompt = """
        You are a helpful assistant answering questions using the provided context.

        Rules:
        - Answer using only the provided context.
        - Write in clear, natural English.
        - Always put spaces between words.
        - Use proper punctuation.
        - Return only plain text.
        - Write complete sentences.
        - Do not merge words together.
        - Do not use Markdown formatting unless necessary.
        - If the answer is not available in the context, say "I don't know based on the provided documents."

        Context:
        %s

        Question:
        %s

        Answer:
        """.formatted(context, question);
        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}