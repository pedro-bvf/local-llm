package com.demo.controller;

import com.demo.assistant.DatabaseAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final DatabaseAssistant assistant;

    @GetMapping("/ask")
    public String askGet(@RequestParam String q) {
        return assistant.chat(q);
    }

    @PostMapping("/ask")
    public String askPost(@RequestBody QuestionRequest request) {
        return assistant.chat(request.question());
    }

    public record QuestionRequest(String question) {}
}
