package com.demo.assistant.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Deterministic {@link ChatModel} stub for tests. Returns a fixed script of responses,
 * one per {@code call(...)} invocation, and records every prompt — so a test can assert how
 * many model round-trips happened (e.g. to prove a self-correcting retry occurred) without
 * needing a running Ollama.
 *
 * <p>Mirrors the LangChain4j sibling's {@code ScriptedChatModel}; only the Spring AI types differ.
 */
public class ScriptedChatModel implements ChatModel {

  private final Deque<String> responses;
  private final List<Prompt> prompts = new ArrayList<>();
  private final ChatOptions options;

  public ScriptedChatModel(String... scriptedResponses) {
    this(ChatOptions.builder().build(), scriptedResponses);
  }

  public ScriptedChatModel(ChatOptions options, String... scriptedResponses) {
    this.options = options;
    this.responses = new ArrayDeque<>(List.of(scriptedResponses));
  }

  @Override
  public ChatResponse call(Prompt prompt) {
    prompts.add(prompt);
    if (responses.isEmpty()) {
      throw new IllegalStateException("ScriptedChatModel ran out of scripted responses after "
          + prompts.size() + " call(s)");
    }
    return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.poll()))));
  }

  @Override
  public Flux<ChatResponse> stream(Prompt prompt) {
    throw new UnsupportedOperationException("ScriptedChatModel does not stream");
  }

  @Override
  public ChatOptions getOptions() {
    return options;
  }

  /** Number of times the model was invoked. */
  public int callCount() {
    return prompts.size();
  }

  public List<Prompt> prompts() {
    return prompts;
  }
}
