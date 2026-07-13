package com.demo.assistant;

import com.demo.model.Category;
import com.demo.model.Product;
import com.demo.model.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the core value of Hibernate Assistant WITHOUT an LLM: the official
 * {@link MetamodelJsonSerializerImpl} turns the JPA metamodel into the context the model
 * would receive. Entities, relationships and the embeddable all appear automatically —
 * nothing about the schema is written by hand.
 *
 * <p>Bootstraps Hibernate directly (no Spring, no Ollama) so the test is fast and isolated.
 */
class MetamodelSerializerTest {

  private static SessionFactory buildSessionFactory() {
    return new Configuration()
        .addAnnotatedClass(Product.class)
        .addAnnotatedClass(Category.class)
        .addAnnotatedClass(Supplier.class) // Address is @Embeddable, mapped via Supplier
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:metamodel-test;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .buildSessionFactory();
  }

  @Test
  void metamodelPromptDescribesTheMappedModelAutomatically() {
    try (SessionFactory sessionFactory = buildSessionFactory()) {
      final String json = MetamodelJsonSerializerImpl.INSTANCE.toString(sessionFactory.getMetamodel());

      // entities
      assertThat(json).contains("Product");
      assertThat(json).contains("Category");
      assertThat(json).contains("Supplier");

      // relationships and the embeddable surface from the mapping, with no manual description
      assertThat(json).contains("category");
      assertThat(json).contains("supplier");
      assertThat(json).contains("address");
      assertThat(json).contains("country");
    }
  }
}
