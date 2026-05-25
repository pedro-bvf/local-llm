package com.demo.tool;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HibernateQueryTool {

  private final HqlAccessValidator validator;
  private final HqlResultFormatter formatter;

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional(readOnly = true)
  public String executeHqlQuery(String hqlQuery, int maxResults) {
    String validationError = validator.validate(hqlQuery);
    if (validationError != null) {
      return validationError;
    }

    try {
      List<?> results = entityManager
        .createQuery(hqlQuery)
        .setMaxResults(Math.min(maxResults, 50))
        .getResultList();

      if (results.isEmpty()) {
        return "No results found for that query.";
      }

      return formatter.format(results);
    } catch (Exception e) {
      return "HQL query error: " + e.getMessage();
    }
  }

}
