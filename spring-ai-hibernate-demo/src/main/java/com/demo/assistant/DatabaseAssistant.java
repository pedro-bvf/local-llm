package com.demo.assistant;

import com.demo.dataaccess.HqlQueryExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseAssistant {

  private final HqlGenerator hqlGenerator;
  private final AnswerFormatter answerFormatter;
  private final HqlQueryExecutor queryExecutor;
  private final HqlSanitizer hqlSanitizer;

  public String chat(String question) {
    QueryPlan queryPlan;
    try {
      queryPlan = buildQueryPlan(question);
    } catch (RuntimeException e) {
      return "The local model is not reachable. Start Ollama and make sure qwen2.5:3b is installed.";
    }

    String result;
    try {
      result = queryExecutor.executeHqlQuery(queryPlan.hql(), queryPlan.maxResults());
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
    return new QueryPlan(hqlSanitizer.sanitizeHql(hqlGenerator.generateHql(question)), 50);
  }

  private String formatAnswer(String question, String hql, String result) {
    if (result.startsWith("ERROR:") || result.startsWith("HQL query error:")) {
      return result;
    }

    return answerFormatter.formatAnswer(question, hql, result);
  }

  private record QueryPlan(String hql, int maxResults) {}
}
