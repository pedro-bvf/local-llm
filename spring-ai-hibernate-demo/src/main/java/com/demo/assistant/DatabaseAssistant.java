package com.demo.assistant;

import com.demo.tool.HibernateQueryTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class DatabaseAssistant {

  private final ChatClient chatClient;
  private final HibernateQueryTool queryTool;

  public DatabaseAssistant(ChatClient.Builder chatClientBuilder, HibernateQueryTool queryTool) {
    this.chatClient = chatClientBuilder.build();
    this.queryTool = queryTool;
  }

  public String chat(String question) {
    QueryPlan queryPlan;
    try {
      queryPlan = buildQueryPlan(question);
    } catch (RuntimeException e) {
      return "The local model is not reachable. Start Ollama and make sure qwen2.5:3b is installed.";
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
    return new QueryPlan(sanitizeHql(generateHql(question)), 50);
  }

  private String generateHql(String question) {
    return chatClient.prompt()
      .system("""
        You convert user questions into one read-only HQL query.
        
        Rules:
        - Output only the HQL query, with no markdown and no explanation.
        - Use entity names Product and Category, not table names.
        - Use Java field names: Product(id, name, price, stock, category, costPrice, supplierCode), Category(id, name).
        - Product.costPrice and Product.supplierCode exist, but are restricted fields.
        - If the user asks for cost price, margin, supplier code, or supplier codes, generate the HQL with the exact restricted field name. The execution layer will reject it.
        - Category has a products collection.
        - Only SELECT or FROM queries are allowed.
        - Never generate UPDATE, DELETE, INSERT, DROP, ALTER or TRUNCATE.
        - HQL does not support LIMIT. For top-N questions, use ORDER BY only.
        - For product counts, use: SELECT COUNT(p) FROM Product p.
        - For category filters, use p.category.name.
        """)
      .user(question)
      .call()
      .content();
  }

  private String formatAnswer(String question, String hql, String result) {
    if (result.startsWith("ERROR:") || result.startsWith("HQL query error:")) {
      return result;
    }

    return chatClient.prompt()
      .system("""
        Answer the user's question using only the real database result.
        
        Rules:
        - Answer in the same language as the user.
        - Do not claim you simulated anything.
        - Do not invent values not present in the database result.
        - Keep the answer concise.
        """)
      .user("""
        User question:
        %s
        
        HQL executed:
        %s
        
        Database result:
        %s
        """.formatted(question, hql, result))
      .call()
      .content();
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

  private record QueryPlan(String hql, int maxResults) {
  }
}
