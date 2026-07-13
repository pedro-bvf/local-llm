package com.demo.assistant;

import com.demo.assistant.support.AssistantTestBase;
import com.demo.assistant.support.ScriptedChatModel;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

class ChatOptionsTest extends AssistantTestBase {

  @Test
  void structuredOutputPreservesConfiguredModel() {
    final OllamaChatOptions configured = OllamaChatOptions.builder()
        .model("qwen2.5-coder:7b")
        .temperature(0.0)
        .build();
    final ScriptedChatModel model = new ScriptedChatModel(
        configured, "{\"hql\":\"SELECT p FROM Product p\"}");
    final HibernateAssistantSpringAI assistant =
        new HibernateAssistantSpringAI(model, sessionFactory().getMetamodel());

    try (StatelessSession session = sessionFactory().openStatelessSession()) {
      assistant.createAiQuery("List the products", session);
    }

    assertThat(model.prompts()).hasSize(1);
    assertThat(model.prompts().getFirst().getOptions())
        .isInstanceOfSatisfying(OllamaChatOptions.class, options -> {
          assertThat(options.getModel()).isEqualTo("qwen2.5-coder:7b");
          assertThat(options.getTemperature()).isEqualTo(0.0);
          assertThat(options.getOutputSchema()).contains("\"hql\"");
        });
  }
}
