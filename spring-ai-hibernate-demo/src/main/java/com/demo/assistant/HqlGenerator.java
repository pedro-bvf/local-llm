package com.demo.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HqlGenerator {

  private final ChatClient chatClient;

  public HqlGenerator(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateHql(String question) {
    return chatClient.prompt()
      .system("""
        You convert user questions into one read-only HQL query.
        
        Rules:
        - Output only the HQL query, with no markdown and no explanation.
        - Never answer that an entity or field is missing; generate HQL and let the execution layer reject it.
        - Generate HQL using Java entity names, not database table names.
        - The known entities are Product and Category.
        - For unknown entities, infer a Java entity name; example: user name -> SELECT u.name FROM User u.
        - Use Java field names: Product(id, name, price, stock, category, costPrice, supplierCode), Category(id, name).
        - Some requested fields may not exist or may not be exposed by the assistant.
        - For unknown Product attributes, infer the field name; example: product size -> SELECT p.size FROM Product p.
        - For inaccessible fields, generate HQL anyway; the execution layer will reject them.
        - Category has a products collection.
        - Only SELECT or FROM queries are allowed.
        - Never generate UPDATE, DELETE, INSERT, DROP, ALTER or TRUNCATE.
        - HQL does not support LIMIT. For top-N questions, use ORDER BY only.
        - For product counts, use: SELECT COUNT(p) FROM Product p.
        - For average price per category, use: SELECT p.category.name, AVG(p.price) FROM Product p GROUP BY p.category.name.
        - For category filters, use p.category.name.
        - Compare relationships through their fields, for example p.category.id or p.category.name.
        """)
      .user(question)
      .call()
      .content();
  }
}
