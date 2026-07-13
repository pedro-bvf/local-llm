package com.demo.config;

import com.demo.assistant.FilteringMetamodelSerializer;
import com.demo.assistant.HibernateAssistantSpringAI;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.tool.language.spi.MetamodelSerializer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AssistantConfig {

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
   *
   * <p>The {@link ChatModel} is the one auto-configured by {@code spring-ai-starter-model-ollama}
   * from the {@code spring.ai.ollama.*} properties — unlike the LangChain4j sibling, which builds
   * its chat model by hand. The assistant only depends on the {@code ChatModel} abstraction, so
   * swapping Ollama for OpenAI/Anthropic is a starter + properties change, with no code edit here.
   */
  @Bean
  HibernateAssistantSpringAI hibernateAssistant(
      ChatModel chatModel,
      EntityManagerFactory entityManagerFactory,
      MetamodelSerializer metamodelSerializer) {
    final Set<String> hiddenResultFields = HIDDEN_FIELDS.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toUnmodifiableSet());
    return new HibernateAssistantSpringAI(
        chatModel, entityManagerFactory.getMetamodel(), metamodelSerializer, hiddenResultFields);
  }
}
