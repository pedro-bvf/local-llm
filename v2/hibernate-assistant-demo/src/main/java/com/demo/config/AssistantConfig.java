package com.demo.config;

import com.demo.assistant.FilteringMetamodelSerializer;
import com.demo.assistant.HibernateAssistantLC4J;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.tool.language.spi.MetamodelSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Configuration
public class AssistantConfig {

  /**
   * Plain LangChain4j Ollama chat model. We build it ourselves (instead of using the
   * LangChain4j Spring Boot starter) to avoid coupling to Spring Boot 4 auto-configuration.
   */
  @Bean
  ChatModel ollamaChatModel(
      @Value("${assistant.ollama.base-url:http://localhost:11434}") String baseUrl,
      @Value("${assistant.ollama.model:qwen2.5-coder:7b}") String model,
      @Value("${assistant.ollama.timeout-seconds:120}") long timeoutSeconds) {
    return OllamaChatModel.builder()
        .baseUrl(baseUrl)
        .modelName(model)
        .temperature(0.0)
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .logRequests(true)
        .logResponses(true)
        .build();
  }

  // ── What the assistant is allowed to see (single source of truth) ───────────
  //   - Product.costPrice : sensitive margin data, mapped but never shown to the model
  //   - AuditLog          : an internal table the assistant must not reach
  private static final Set<String> HIDDEN_ENTITIES = Set.of("AuditLog");
  private static final Map<String, Set<String>> HIDDEN_FIELDS = Map.of("Product", Set.of("costPrice"));

  /**
   * Curates what the LLM is told about the domain. Built on Hibernate Assistant's own serializer;
   * unlike an MCP on the raw database (which sees every table/column), the surface is decided here.
   */
  @Bean
  MetamodelSerializer metamodelSerializer() {
    return new FilteringMetamodelSerializer(HIDDEN_ENTITIES, HIDDEN_FIELDS);
  }

  /**
   * The assistant is built from the JPA {@link jakarta.persistence.metamodel.Metamodel}, filtered
   * through {@link #metamodelSerializer()}. The same hidden fields are also stripped from result
   * JSON, so a whole-entity {@code SELECT p} cannot leak a hidden column either.
   */
  @Bean
  HibernateAssistantLC4J hibernateAssistant(
      ChatModel chatModel,
      EntityManagerFactory entityManagerFactory,
      MetamodelSerializer metamodelSerializer) {
    final Set<String> hiddenResultFields = HIDDEN_FIELDS.values().stream()
        .flatMap(Set::stream)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    return new HibernateAssistantLC4J(
        chatModel, entityManagerFactory.getMetamodel(), metamodelSerializer, hiddenResultFields);
  }
}
