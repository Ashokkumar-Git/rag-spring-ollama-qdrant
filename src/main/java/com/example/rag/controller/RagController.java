package com.example.rag.controller;

import com.example.rag.contract.RagRequest;
import com.example.rag.contract.RagResponse;
import com.example.rag.service.RagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/ask")
    public String ask(
            @RequestParam String question) {

        return ragService.ask(question);
    }

    @PostMapping("/ask")
    public RagResponse ask(@RequestBody RagRequest request) {

        String answer = ragService.ask(request.question());

        return new RagResponse(answer);
    }
}