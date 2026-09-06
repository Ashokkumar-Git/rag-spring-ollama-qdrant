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
                Answer the question using the context below.
                
                CONTEXT:
                %s
                
                QUESTION:
                %s
                
                Rules:
                - Answer using only the context.
                - Use proper Markdown formatting.
                - Use headings, bullet points, and tables when useful.
                - Use normal spaces between words.
                - Do not use HTML.
                - If the answer is unavailable, say:
                  "I don't know based on the provided documents."
                
                ANSWER:
                """.formatted(context, question);

        System.out.println(prompt);
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        System.out.println(response);
        return response;
    }
}