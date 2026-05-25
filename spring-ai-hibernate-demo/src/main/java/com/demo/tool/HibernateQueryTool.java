package com.demo.tool;

import com.demo.model.Category;
import com.demo.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
   * Matches entity names after FROM and JOIN / JOIN FETCH keywords.
   */
  private static final Pattern ENTITY_PATTERN =
    Pattern.compile("(?i:FROM|JOIN(?:\\s+FETCH)?)\\s+([A-Z]\\w*)");

  /**
   * Fields exposed to the AI assistant for each allowed entity. Any mapped,
   * unmapped, or hallucinated field outside this set is blocked before
   * Hibernate tries to parse the HQL.
   */
  private static final Map<String, Set<String>> ALLOWED_FIELDS_BY_ENTITY = Map.of(
    "Product", Set.of("id", "name", "price", "stock", "category"),
    "Category", Set.of("id", "name", "products")
  );

  /**
   * Matches entity aliases declared in FROM clauses, e.g. FROM Product p.
   */
  private static final Pattern ENTITY_ALIAS_PATTERN =
    Pattern.compile("(?i)\\bFROM\\s+([A-Z]\\w*)\\s+(\\w+)");

  /**
   * Matches alias.field references, e.g. p.name or Product.name.
   */
  private static final Pattern FIELD_REFERENCE_PATTERN =
    Pattern.compile("\\b([A-Za-z]\\w*)\\.([A-Za-z]\\w*)\\b");

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional(readOnly = true)
  public String executeHqlQuery(String hqlQuery, int maxResults) {
    // Layer 1a: entity whitelist — reject queries referencing non-allowed entities
    String whitelistError = validateEntityWhitelist(hqlQuery);
    if (whitelistError != null) return whitelistError;

    // Layer 1b: field blacklist — reject queries referencing restricted fields
    String unknownFieldError = validateFieldWhitelist(hqlQuery);
    if (unknownFieldError != null) return unknownFieldError;

    // Layer 1b: field whitelist — reject unknown or non-exposed fields
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
   * Checks alias.field references against the fields exposed to the AI.
   * This turns hallucinated fields such as p.color into a controlled assistant
   * error instead of a lower-level Hibernate exception.
   */
  private String validateFieldWhitelist(String hqlQuery) {
    Map<String, String> aliases = new HashMap<>();
    ALLOWED_ENTITIES.forEach(entity -> aliases.put(entity, entity));

    Matcher aliasMatcher = ENTITY_ALIAS_PATTERN.matcher(hqlQuery);
    while (aliasMatcher.find()) {
      aliases.put(aliasMatcher.group(2), aliasMatcher.group(1));
    }

    Matcher fieldMatcher = FIELD_REFERENCE_PATTERN.matcher(hqlQuery);
    while (fieldMatcher.find()) {
      String alias = fieldMatcher.group(1);
      String field = fieldMatcher.group(2);
      String entity = aliases.get(alias);
      Set<String> allowedFields = ALLOWED_FIELDS_BY_ENTITY.get(entity);

      if (entity != null && !allowedFields.contains(field)) {
        return "ERROR: Field '" + field + "' from entity '" + entity + "' is not accessible via this assistant.";
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
