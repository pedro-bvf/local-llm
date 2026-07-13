package com.demo.assistant.support;

import com.demo.model.AuditLog;
import com.demo.model.Category;
import com.demo.model.Product;
import com.demo.model.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.function.Function;

/**
 * Base for deterministic tests that exercise the <em>real</em> Hibernate Assistant pieces
 * (HQL validation via {@code createSelectionQuery}, result serialization) against an
 * in-memory H2 seeded from the demo's own {@code data.sql}. No Spring, no Ollama.
 */
public abstract class AssistantTestBase {

  private static volatile SessionFactory sessionFactory;

  protected static synchronized SessionFactory sessionFactory() {
    if (sessionFactory == null) {
      sessionFactory = new Configuration()
          .addAnnotatedClass(Product.class)
          .addAnnotatedClass(Category.class)
          .addAnnotatedClass(Supplier.class) // Address is @Embeddable, mapped via Supplier
          .addAnnotatedClass(AuditLog.class) // internal table; present in the model, hidden from the LLM
          .setProperty("hibernate.connection.url", "jdbc:h2:mem:assistant-tests;DB_CLOSE_DELAY=-1")
          .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
          .setProperty("hibernate.connection.username", "sa")
          .setProperty("hibernate.hbm2ddl.auto", "create-drop")
          // reuse the demo's seed data as the test fixture (single source of truth)
          .setProperty("hibernate.hbm2ddl.import_files", "data.sql")
          .buildSessionFactory();
    }
    return sessionFactory;
  }

  /** Runs work inside a short-lived stateless session and a read-only-style transaction. */
  protected <R> R inSession(Function<StatelessSession, R> work) {
    try (StatelessSession session = sessionFactory().openStatelessSession()) {
      final Transaction tx = session.beginTransaction();
      try {
        final R result = work.apply(session);
        tx.commit();
        return result;
      } catch (RuntimeException e) {
        if (tx.isActive()) {
          tx.rollback();
        }
        throw e;
      }
    }
  }
}
