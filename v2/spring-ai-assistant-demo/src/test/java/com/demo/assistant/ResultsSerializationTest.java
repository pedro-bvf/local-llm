package com.demo.assistant;

import com.demo.assistant.support.AssistantTestBase;
import com.demo.assistant.support.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.hibernate.query.SelectionQuery;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic checks of the data path with a KNOWN HQL query (the LLM is bypassed via a
 * no-op {@link ScriptedChatModel}). This validates that Hibernate executes the HQL and that
 * the official {@code ResultsJsonSerializer} renders the results — including across the
 * {@code supplier} relationship and the {@code address} embeddable.
 */
class ResultsSerializationTest extends AssistantTestBase {

  // chat model is never called on this path, so an empty script is fine
  private final HibernateAssistantSpringAI assistant =
      new HibernateAssistantSpringAI(new ScriptedChatModel(), sessionFactory().getMetamodel());

  private String runToJson(String hql) {
    return inSession(session -> {
      final SelectionQuery<?> query = session.createSelectionQuery(hql, Object.class);
      try {
        return assistant.executeQueryToJson(query, session);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void countMatchesSeedData() {
    assertThat(runToJson("SELECT COUNT(p) FROM Product p")).contains("25");
  }

  @Test
  void topByPriceReturnsMacBookFirst() {
    final String json = runToJson("SELECT p.name FROM Product p ORDER BY p.price DESC");
    assertThat(json).contains("MacBook Pro");
    assertThat(json.indexOf("MacBook Pro")).isLessThan(json.indexOf("iPhone 15 Pro"));
  }

  @Test
  void embeddableCountryFilterAcrossRelationship() {
    // p.supplier (relationship) + s.address.country (embeddable) — known only from the metamodel
    final String json = runToJson(
        "SELECT p.name FROM Product p WHERE p.supplier.address.country = 'Switzerland'");
    assertThat(json).contains("Logitech MX Keys").contains("USB-C Hub 7-in-1");
  }

  @Test
  void supplierInItaly() {
    assertThat(runToJson("SELECT s.name FROM Supplier s WHERE s.address.country = 'Italy'"))
        .contains("EuroDistribution Srl");
  }

  @Test
  void groupByCategory() {
    final String json = runToJson(
        "SELECT p.category.name, COUNT(p) FROM Product p GROUP BY p.category.name");
    assertThat(json).contains("Electronics").contains("Books");
  }

  @Test
  void wholeEntitySelectDoesNotLeakHiddenField() {
    // assistant configured to hide Product.costPrice from results
    final HibernateAssistantSpringAI hiding = new HibernateAssistantSpringAI(
        new ScriptedChatModel(), sessionFactory().getMetamodel(),
        new FilteringMetamodelSerializer(Set.of(), Map.of("Product", Set.of("costPrice"))),
        Set.of("costPrice"));

    final String json = inSession(session -> {
      final SelectionQuery<?> query = session.createSelectionQuery("SELECT p FROM Product p", Object.class);
      try {
        return hiding.executeQueryToJson(query, session);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    assertThat(json).doesNotContain("costPrice");
    assertThat(json).contains("name").contains("price").contains("stock"); // the rest is intact
  }
}
