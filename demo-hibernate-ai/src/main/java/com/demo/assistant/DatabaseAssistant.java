package com.demo.assistant;

import com.demo.tool.HibernateQueryTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseAssistant {

  private final HqlGenerator hqlGenerator;
  private final AnswerFormatter answerFormatter;
  private final HibernateQueryTool queryTool;

  public String chat(String question) {
    QueryPlan queryPlan;
    try {
      queryPlan = buildQueryPlan(question);
    } catch (RuntimeException e) {
      return localModelUnavailableMessage();
    }

    String result;
    try {
      result = queryTool.executeHqlQuery(queryPlan.hql(), queryPlan.maxResults());
    } catch (RuntimeException e) {
      return "HQL query error: " + e.getMessage();
    }

    try {
      return formatAnswer(question, queryPlan.hql(), result);
    } catch (RuntimeException e) {
      return result;
    }
  }

  private QueryPlan buildQueryPlan(String question) {
    return new QueryPlan(sanitizeHql(hqlGenerator.generateHql(question)), 50);
  }

  private String formatAnswer(String question, String hql, String result) {
    if (result.startsWith("ERROR:") || result.startsWith("HQL query error:")) {
      return result;
    }

    return answerFormatter.formatAnswer(question, hql, result);
  }

  /**
   * Normalizes the raw HQL string returned by the LLM before passing it to Hibernate.
   */
  private String sanitizeHql(String rawHql) {
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

  private String localModelUnavailableMessage() {
    return "The local model is not reachable. Start Ollama and make sure qwen2.5:3b is installed.";
  }

  private record QueryPlan(String hql, int maxResults) {}
}
