package com.demo.controller;

import com.demo.assistant.HibernateAssistantLC4J;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * REST entry point. Each request runs inside a short-lived {@link StatelessSession} and a
 * read-only transaction; the session is handed to the assistant so Hibernate parses,
 * validates and executes the generated HQL.
 *
 * <pre>
 *   GET /assistant/ask?q=...    natural-language question -> natural-language answer
 *   GET /assistant/hql?q=...    natural-language question -> generated HQL + raw JSON data
 *   GET /assistant/metamodel    the exact metamodel view the LLM is given (after hiding)
 *   GET /assistant/clear        reset the chat context (keeps the metamodel system prompt)
 * </pre>
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

  private static final Logger log = LoggerFactory.getLogger(AssistantController.class);

  private final SessionFactory sessionFactory;
  private final HibernateAssistantLC4J assistant;
  private final String ollamaBaseUrl;
  private final String ollamaModel;

  public AssistantController(
      EntityManagerFactory entityManagerFactory,
      HibernateAssistantLC4J assistant,
      @Value("${assistant.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
      @Value("${assistant.ollama.model:qwen2.5-coder:7b}") String ollamaModel) {
    this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    this.assistant = assistant;
    this.ollamaBaseUrl = ollamaBaseUrl;
    this.ollamaModel = ollamaModel;
  }

  @GetMapping("/ask")
  public String ask(@RequestParam("q") String q) {
    assistant.clear(); // each REST call is independent — start from just the metamodel prompt
    return guarded(() -> inSession(session -> assistant.executeQuery(q, session)));
  }

  @GetMapping("/hql")
  public String hql(@RequestParam("q") String q) {
    assistant.clear(); // each REST call is independent — start from just the metamodel prompt
    return guarded(() -> inSession(session -> {
      final SelectionQuery<?> query = assistant.createAiQuery(q, session);
      final String generatedHql = ((SqmQuery<?>) query).getQueryString();
      final String json;
      try {
        json = assistant.executeQueryToJson(query, session);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      return "HQL: " + generatedHql + "\n\n" + json;
    }));
  }

  /** Shows exactly what the model is told about the domain — hidden fields/tables are absent here. */
  @GetMapping("/metamodel")
  public String metamodel() {
    return assistant.describeMetamodel();
  }

  @GetMapping("/clear")
  public String clear() {
    assistant.clear();
    return "Assistant context cleared.";
  }

  private <R> R inSession(Function<StatelessSession, R> work) {
    try (StatelessSession session = sessionFactory.openStatelessSession()) {
      final Transaction tx = session.beginTransaction();
      try {
        final R result = work.apply(session);
        tx.commit();
        return result;
      } catch (RuntimeException e) {
        if (tx.isActive()) {
          tx.rollback();
        }
        throw e;
      }
    }
  }

  /**
   * Keeps demo output readable and, crucially, distinguishes the two very different failure modes:
   * the model being unreachable (a connectivity problem) versus the assistant being unable to answer
   * from the data it is allowed to see (e.g. a query Hibernate can't resolve). Only the former gets
   * the "is Ollama running?" hint.
   */
  private String guarded(Callable<String> action) {
    try {
      return action.call();
    } catch (Exception e) {
      log.error("Assistant request failed", e);
      if (looksLikeModelUnreachable(e)) {
        return "ERROR: the language model is not reachable.\n"
            + "(Is Ollama running at " + ollamaBaseUrl + " and is the model '" + ollamaModel + "' pulled?)";
      }
      return "I couldn't answer that from the data available to me.\nReason: " + rootMessage(e);
    }
  }

  /** True only for transport/connectivity failures talking to the model — not query/answer errors. */
  private static boolean looksLikeModelUnreachable(Throwable e) {
    for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
      if (t instanceof IOException) {
        return true; // ConnectException / SocketTimeoutException / UnknownHostException all extend this
      }
      final String m = t.getMessage();
      if (m != null) {
        final String lower = m.toLowerCase(Locale.ROOT);
        if (lower.contains("connection refused") || lower.contains("failed to connect")
            || lower.contains("connect timed out") || lower.contains("timed out")
            || lower.contains("no route to host") || lower.contains("unknownhost")
            || lower.contains("11434")) {
          return true;
        }
      }
    }
    return false;
  }

  /** Deepest cause message, for a concise human-readable reason. */
  private static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null && t.getCause() != t) {
      t = t.getCause();
    }
    final String m = t.getMessage();
    return m != null ? m : t.getClass().getSimpleName();
  }
}
