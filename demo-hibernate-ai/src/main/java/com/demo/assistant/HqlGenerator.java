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
            - Use entity names Product and Category, not table names.
            - Use Java field names: Product(id, name, price, stock, category), Category(id, name).
            - Category has a products collection.
            - Only SELECT or FROM queries are allowed.
            - Never generate UPDATE, DELETE, INSERT, DROP, ALTER or TRUNCATE.
            - HQL does not support LIMIT. For top-N questions, use ORDER BY only.
            - For product counts, use: SELECT COUNT(p) FROM Product p
            - For category filters, use p.category.name.
            - Compare relationships through their fields, for example p.category.id or p.category.name.
            """)
    @UserMessage("{{question}}")
    String generateHql(@V("question") String question);
}
