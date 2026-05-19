package com.demo.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.demo.model.Category;
import com.demo.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool that executes HQL queries against the database.
 * <p>
 * The LLM never touches raw SQL. Hibernate validates HQL against the entity
 * model before execution, keeping queries type-aware and portable.
 * <p>
 * Security restrictions:
 *  - Only SELECT and FROM read-only queries are allowed
 *  - UPDATE / DELETE / INSERT attempts are blocked here
 */
@Component
public class HibernateQueryTool {

    @PersistenceContext
    private EntityManager entityManager;

    @Tool("""
            Execute an HQL (Hibernate Query Language) query to retrieve data from the database.

            Rules you MUST follow:
            - Only use SELECT or FROM queries (read-only)
            - Use entity names as defined in Java (Product, Category), NOT table names
            - Use field names as defined in Java (e.g. p.price, p.category.name)
            - Never use UPDATE, DELETE, INSERT or DDL statements
            - For joins, prefer JPQL join syntax: JOIN FETCH p.category c

            The database has these entities:
            - Product(id, name, price, stock, category)
            - Category(id, name)
            Category has a OneToMany relationship with Product (category.products).
            """)
    public String executeHqlQuery(
            @P(value = "A read-only HQL query using the Product and Category entities", required = true)
            String hqlQuery
    ) {
        return executeHqlQuery(hqlQuery, 50);
    }

    public String executeHqlQuery(String hqlQuery, int maxResults) {
        // Safety guard: block writes.
        String query = hqlQuery.trim().toUpperCase();
        if (query.startsWith("UPDATE") || query.startsWith("DELETE")
                || query.startsWith("INSERT") || query.startsWith("DROP")
                || query.startsWith("ALTER") || query.startsWith("TRUNCATE")) {
            return "ERROR: Only read-only (SELECT) queries are allowed. "
                    + "This assistant cannot modify data.";
        }

        if (!query.startsWith("SELECT") && !query.startsWith("FROM")) {
            return "ERROR: Only read-only SELECT or FROM HQL queries are allowed.";
        }

        try {
            List<?> results = entityManager
                    .createQuery(hqlQuery)
                    .setMaxResults(Math.min(maxResults, 50))          // Safety limit
                    .getResultList();

            if (results.isEmpty()) {
                return "No results found for that query.";
            }

            return results.stream()
                    .map(this::formatResult)
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "HQL query error: " + e.getMessage()
                    + "\nPlease review the query and try again.";
        }
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
