package com.demo.assistant;

import org.springframework.stereotype.Component;

@Component
public class HqlSanitizer {

  /**
   * Normalizes the raw HQL string returned by the LLM before passing it to Hibernate.
   */
  public String sanitizeHql(String rawHql) {
    String hql = rawHql.trim();

    if (hql.startsWith("```")) {
      hql = hql.replaceFirst("(?is)^```(?:hql|jpql|sql)?\\s*", "");
      hql = hql.replaceFirst("(?is)\\s*```$", "");
    }

    int semicolon = hql.indexOf(';');
    if (semicolon >= 0) {
      hql = hql.substring(0, semicolon);
    }

    return hql.trim();
  }
}
