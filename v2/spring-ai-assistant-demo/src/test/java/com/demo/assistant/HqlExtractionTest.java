package com.demo.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for HQL extraction from the model response. Pure function, no Hibernate, no LLM.
 * Covers the structured-JSON path and the plain-text fallback (Spring Boot 4 ships Jackson 3,
 * so the extractor is deliberately dependency-free).
 */
class HqlExtractionTest {

  @Test
  void extractsFromStructuredJson() {
    assertThat(HibernateAssistantSpringAI.extractHql("{\"hql\":\"SELECT p FROM Product p\"}"))
        .isEqualTo("SELECT p FROM Product p");
  }

  @Test
  void extractsFromJsonWithEscapedQuotesAndWhitespace() {
    final String response = "  {\n  \"hql\" :  \"SELECT s FROM Supplier s WHERE s.address.country = 'Italy'\"\n}";
    assertThat(HibernateAssistantSpringAI.extractHql(response))
        .isEqualTo("SELECT s FROM Supplier s WHERE s.address.country = 'Italy'");
  }

  @Test
  void fallsBackToPlainText() {
    assertThat(HibernateAssistantSpringAI.extractHql("SELECT COUNT(p) FROM Product p"))
        .isEqualTo("SELECT COUNT(p) FROM Product p");
  }

  @Test
  void fallsBackThroughCodeFences() {
    assertThat(HibernateAssistantSpringAI.extractHql("```hql\nSELECT p FROM Product p\n```"))
        .isEqualTo("SELECT p FROM Product p");
  }

  @Test
  void failsWhenNoQueryPresent() {
    assertThatThrownBy(() -> HibernateAssistantSpringAI.extractHql("I'm sorry, I cannot help with that."))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void failsOnEmptyResponse() {
    assertThatThrownBy(() -> HibernateAssistantSpringAI.extractHql("  "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
