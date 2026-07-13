package com.demo.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AnswerFormatter {

  private final ChatClient chatClient;

  public AnswerFormatter(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String formatAnswer(String question, String hql, String result) {
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
}
