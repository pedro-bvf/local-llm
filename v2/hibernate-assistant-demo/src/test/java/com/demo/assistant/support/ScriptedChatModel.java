package com.demo.assistant.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Deterministic {@link ChatModel} stub for tests. Returns a fixed script of responses,
 * one per {@code chat(...)} call, and records every request — so a test can assert how
 * many model round-trips happened (e.g. to prove a self-correcting retry occurred)
 * without needing a running Ollama.
 */
public class ScriptedChatModel implements ChatModel {

  private final Deque<String> responses;
  private final List<ChatRequest> requests = new ArrayList<>();

  public ScriptedChatModel(String... scriptedResponses) {
    this.responses = new ArrayDeque<>(List.of(scriptedResponses));
  }

  @Override
  public ChatResponse chat(ChatRequest chatRequest) {
    requests.add(chatRequest);
    if (responses.isEmpty()) {
      throw new IllegalStateException("ScriptedChatModel ran out of scripted responses after "
          + requests.size() + " call(s)");
    }
    return ChatResponse.builder()
        .aiMessage(AiMessage.from(responses.poll()))
        .build();
  }

  /** Number of times the model was invoked. */
  public int callCount() {
    return requests.size();
  }

  public List<ChatRequest> requests() {
    return requests;
  }
}
