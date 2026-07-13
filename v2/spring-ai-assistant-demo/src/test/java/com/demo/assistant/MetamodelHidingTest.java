package com.demo.assistant;

import com.demo.assistant.support.AssistantTestBase;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the "hide tables/fields from the LLM" virtue deterministically (no Ollama): the default
 * Hibernate serializer exposes everything, while {@link FilteringMetamodelSerializer} removes the
 * sensitive field {@code Product.costPrice} and the whole {@code AuditLog} entity from what the
 * model is told — while keeping the rest intact.
 */
class MetamodelHidingTest extends AssistantTestBase {

  private final FilteringMetamodelSerializer filtered = new FilteringMetamodelSerializer(
      Set.of("AuditLog"),
      Map.of("Product", Set.of("costPrice")));

  @Test
  void defaultSerializerExposesEverything() {
    final String full = MetamodelJsonSerializerImpl.INSTANCE.toString(sessionFactory().getMetamodel());
    assertThat(full).contains("costPrice");
    assertThat(full).contains("AuditLog");
  }

  @Test
  void filteringSerializerHidesSensitiveFieldAndInternalTable() {
    final String view = filtered.toString(sessionFactory().getMetamodel());

    // hidden from the LLM entirely
    assertThat(view).doesNotContain("costPrice");
    assertThat(view).doesNotContain("AuditLog");
    assertThat(view).doesNotContain("performed_by").doesNotContain("performedBy");

    // everything legitimate is still there
    assertThat(view).contains("Product");
    assertThat(view).contains("Category");
    assertThat(view).contains("Supplier");
    assertThat(view).contains("price");
    assertThat(view).contains("stock");
    assertThat(view).contains("category");
    assertThat(view).contains("supplier");
    assertThat(view).contains("address");
    assertThat(view).contains("country");
  }
}
