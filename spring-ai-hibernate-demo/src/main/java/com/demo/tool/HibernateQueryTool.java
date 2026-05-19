package com.demo.tool;

import com.demo.model.Category;
import com.demo.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HibernateQueryTool {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public String executeHqlQuery(String hqlQuery, int maxResults) {
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
