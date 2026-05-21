package com.demo.assistant;

import com.demo.tool.HibernateQueryTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Demo assistant.
 * <p>
 * Coordinates declarative LangChain4j AI services with the Hibernate query tool.
 */
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

    String result = queryTool.executeHqlQuery(queryPlan.hql(), queryPlan.maxResults());
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
   <p>This method applies two normalisation steps:
   * <ol>
   *   <li>Strip markdown fences — removes the opening {@code ```[hql|jpql|sql]} and
   *       closing {@code ```} if present.</li>
   *   <li>Truncate at the first semicolon — JPQL does not use statement
   *       terminators; a trailing {@code ;} causes a parse error in Hibernate.</li>
   * </ol>
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

  private record QueryPlan(String hql, int maxResults) {
  }
}
