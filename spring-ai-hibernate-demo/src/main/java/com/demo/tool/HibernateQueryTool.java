package com.demo.tool;

import com.demo.model.Category;
import com.demo.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class HibernateQueryTool {

  /**
   * Entities the AI assistant is allowed to query.
   * Any @Entity not listed here is blocked, even if fully mapped in the
   * application. Add or remove entries here to control AI visibility.
   */
  private static final Set<String> ALLOWED_ENTITIES = Set.of("Product", "Category");
  /**
   * Fields the AI assistant is NOT allowed to reference, even within
   * an allowed entity. Blocks use in SELECT, WHERE, ORDER BY, etc.
   * These fields may still exist in the @Entity and be used by the
   * application — the restriction applies only to AI-generated queries.
   */
  private static final Set<String> RESTRICTED_FIELDS = Set.of("costPrice", "supplierCode");
  /**
   * Matches entity names after FROM and JOIN / JOIN FETCH keywords.
   */
  private static final Pattern ENTITY_PATTERN =
    Pattern.compile("(?:FROM|JOIN(?:\\s+FETCH)?)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional(readOnly = true)
  public String executeHqlQuery(String hqlQuery, int maxResults) {
    // Layer 1a: entity whitelist — reject queries referencing non-allowed entities
    String whitelistError = validateEntityWhitelist(hqlQuery);
    if (whitelistError != null) return whitelistError;

    // Layer 1b: field blacklist — reject queries referencing restricted fields
    String fieldError = validateFieldBlacklist(hqlQuery);
    if (fieldError != null) return fieldError;

    // Layer 2: write guard — rejects non-SELECT statements
    String query = hqlQuery.trim().toUpperCase();
    if (query.startsWith("UPDATE")
      || query.startsWith("DELETE")
      || query.startsWith("INSERT")
      || query.startsWith("DROP")
      || query.startsWith("ALTER")
      || query.startsWith("TRUNCATE")) {
      return "ERROR: Only read-only SELECT queries are allowed.";
    }

    if (!query.startsWith("SELECT") && !query.startsWith("FROM")) {
      return "ERROR: Only read-only SELECT or FROM HQL queries are allowed.";
    }

    try {
      List<?> results = entityManager
        .createQuery(hqlQuery)
        .setMaxResults(Math.min(maxResults, 50))
        .getResultList();

      if (results.isEmpty()) {
        return "No results found for that query.";
      }

      return results.stream()
        .map(this::formatResult)
        .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      return "HQL query error: " + e.getMessage();
    }
  }

  /**
   * Checks that every entity referenced in the HQL query is in
   * {@link #ALLOWED_ENTITIES}. Returns an error string if any forbidden
   * entity is found, or {@code null} if the query is allowed.
   */
  private String validateEntityWhitelist(String hqlQuery) {
    Matcher m = ENTITY_PATTERN.matcher(hqlQuery);
    while (m.find()) {
      String entity = m.group(1);
      if (!ALLOWED_ENTITIES.contains(entity)) {
        return "ERROR: Entity '" + entity + "' is not accessible via this assistant. "
          + "Allowed entities: " + ALLOWED_ENTITIES;
      }
    }
    return null;
  }

  /**
   * Checks that the HQL query does not reference any field in
   * {@link #RESTRICTED_FIELDS}. Word-boundary matching catches references
   * in SELECT, WHERE, ORDER BY, and expressions (e.g. {@code p.costPrice}).
   * Returns an error string if a restricted field is found, or {@code null}.
   */
  private String validateFieldBlacklist(String hqlQuery) {
    for (String field : RESTRICTED_FIELDS) {
      Pattern p = Pattern.compile("\\b" + Pattern.quote(field) + "\\b",
        Pattern.CASE_INSENSITIVE);
      if (p.matcher(hqlQuery).find()) {
        return "ERROR: Field '" + field + "' is not accessible via this assistant.";
      }
    }
    return null;
  }

  private String formatResult(Object result) {
    if (result instanceof Product product) {
      String categoryName = product.getCategory() == null ? null : product.getCategory().getName();
      return "Product{name='%s', price=%s, stock=%d, category='%s'}"
        .formatted(product.getName(), product.getPrice(), product.getStock(), categoryName);
    }

    if (result instanceof Category category) {
      return "Category{name='%s'}".formatted(category.getName());
    }

    if (result instanceof Object[] row) {
      return Arrays.stream(row)
        .map(String::valueOf)
        .collect(Collectors.joining(" | "));
    }

    return String.valueOf(result);
  }
}
