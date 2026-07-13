package com.demo.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "ollamaChatModel")
public interface HqlGenerator {

    @SystemMessage("""
            You convert user questions into one read-only HQL query.

            Rules:
            - Output only the HQL query, with no markdown and no explanation.
            - Never answer that an entity or field is missing; generate HQL and let the execution layer reject it.
            - Generate HQL using Java entity names, not database table names.
            - The known entities are Product and Category.
            - For unknown entities, infer a Java entity name; example: user name -> SELECT u.name FROM User u.
            - Use Java field names: Product(id, name, price, stock, category), Category(id, name).
            - Some requested fields may not exist or may not be exposed by the assistant.
            - For unknown Product attributes, infer the field name; example: product size -> SELECT p.size FROM Product p.
            - For inaccessible fields, generate HQL anyway; the execution layer will reject them.
            - Category has a products collection.
            - Only SELECT or FROM queries are allowed.
            - Never generate UPDATE, DELETE, INSERT, DROP, ALTER or TRUNCATE.
            - HQL does not support LIMIT. For top-N questions, use ORDER BY only.
            - For product counts, use: SELECT COUNT(p) FROM Product p
            - For average price per category, use: SELECT p.category.name, AVG(p.price) FROM Product p GROUP BY p.category.name.
            - For category filters, use p.category.name.
            - Compare relationships through their fields, for example p.category.id or p.category.name.
            """)
    @UserMessage("{{question}}")
    String generateHql(@V("question") String question);
}
