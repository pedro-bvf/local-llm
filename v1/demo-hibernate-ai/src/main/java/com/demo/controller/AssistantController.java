package com.demo.controller;

import com.demo.assistant.DatabaseAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for the assistant.
 * <p>
 * Usage:
 *   GET  /assistant/ask?q=How many products do we have?
 *   POST /assistant/ask  { "question": "What are the cheapest products?" }
 */
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final DatabaseAssistant assistant;

    /** GET for quick browser or curl usage. */
    @GetMapping("/ask")
    public String askGet(@RequestParam String q) {
        return assistant.chat(q);
    }

    /** POST for clients such as Postman or a frontend. */
    @PostMapping("/ask")
    public String askPost(@RequestBody QuestionRequest request) {
        return assistant.chat(request.question());
    }

    public record QuestionRequest(String question) {}
}
