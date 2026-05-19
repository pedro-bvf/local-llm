package com.demo.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "ollamaChatModel")
public interface AnswerFormatter {

    @SystemMessage("""
            Answer the user's question using only the real database result.

            Rules:
            - Answer in the same language as the user.
            - Do not claim you simulated anything.
            - Do not invent values not present in the database result.
            - Keep the answer concise.
            """)
    @UserMessage("""
            User question:
            {{question}}

            HQL executed:
            {{hql}}

            Database result:
            {{result}}
            """)
    String formatAnswer(
            @V("question") String question,
            @V("hql") String hql,
            @V("result") String result
    );
}
