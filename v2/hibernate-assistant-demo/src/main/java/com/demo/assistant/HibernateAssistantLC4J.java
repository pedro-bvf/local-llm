package com.demo.assistant;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.SelectionQuery;
import org.hibernate.tool.language.HibernateAssistant;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;
import org.hibernate.tool.language.internal.ResultsJsonSerializerImpl;
import org.hibernate.tool.language.spi.MetamodelSerializer;
import org.hibernate.tool.language.spi.ResultsSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LangChain4j-based orchestrator that implements Hibernate's
 * {@link org.hibernate.tool.language.HibernateAssistant} interface.
 *
 * <p>The genuinely valuable parts come straight from the official
 * {@code org.hibernate.orm:hibernate-assistant} artifact:
 * <ul>
 *   <li>{@link MetamodelJsonSerializerImpl} turns Hibernate's JPA metamodel into the
 *       system prompt — the LLM learns the entities, fields, relationships and
 *       embeddables from the mapping, with <b>no hand-written schema</b>.</li>
 *   <li>HQL is handed to {@link SharedSessionContract#createSelectionQuery} so
 *       <b>Hibernate itself parses and validates it</b> — no regex guard.</li>
 *   <li>{@link ResultsJsonSerializerImpl} renders results using Hibernate's mapping
 *       knowledge (no entity-graph circularity) before they are fed back to the LLM.</li>
 * </ul>
 *
 * <p>This class only contributes the thin LLM glue (chat model call, JSON-schema
 * response, chat memory, a self-correcting retry). It is adapted, with attribution,
 * from Marco Belladelli's reference demo — see {@code NOTICE}. The original is a CDI
 * bean for Quarkus; this version is plain Spring.
 */
public class HibernateAssistantLC4J implements HibernateAssistant {

  private static final Logger log = LoggerFactory.getLogger(HibernateAssistantLC4J.class);

  /** Same instruction the official library uses; {@code {{it}}} is replaced by the metamodel JSON. */
  private static final String METAMODEL_PROMPT = """
      You are an expert in writing Hibernate Query Language (HQL) queries.
      You have access to an entity model with the following structure:

      %s

      If a user asks a question that can be answered by querying this model, generate an HQL SELECT query.
      The query must not include any input parameters.
      Do not output anything else aside from a valid HQL statement, no explanation, and do not put the query in backticks or code blocks.
      """;

  private static final Pattern HQL_PATTERN = Pattern.compile("(?is)\\bSELECT\\b.*?(?:;|\\n|$)");

  /** One retry: if Hibernate rejects the HQL, the parse error is fed back to the model. */
  private static final int MAX_ATTEMPTS = 2;

  private final ChatModel chatModel;
  private final ChatMemory chatMemory;
  private final JpaMetamodel metamodel;
  private final SystemMessage metamodelPrompt;
  private final String metamodelJson;
  private final Set<String> hiddenResultFields;

  /** Uses Hibernate Assistant's default serializer (the LLM sees the whole mapped model). */
  public HibernateAssistantLC4J(ChatModel chatModel, Metamodel metamodel) {
    this(chatModel, metamodel, MetamodelJsonSerializerImpl.INSTANCE, Set.of());
  }

  public HibernateAssistantLC4J(ChatModel chatModel, Metamodel metamodel, MetamodelSerializer metamodelSerializer) {
    this(chatModel, metamodel, metamodelSerializer, Set.of());
  }

  /**
   * @param metamodelSerializer decides what the LLM is <em>told</em> about the domain (pass a
   *        {@link FilteringMetamodelSerializer} to hide tables/fields from the prompt).
   * @param hiddenResultFields attribute names stripped from the result JSON, so a whole-entity
   *        {@code SELECT p} cannot leak a hidden field that the model was never told about.
   */
  public HibernateAssistantLC4J(
      ChatModel chatModel,
      Metamodel metamodel,
      MetamodelSerializer metamodelSerializer,
      Set<String> hiddenResultFields) {
    this.chatModel = chatModel;
    this.metamodel = (JpaMetamodel) metamodel;
    this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
    this.hiddenResultFields = hiddenResultFields == null ? Set.of() : hiddenResultFields;

    // THE virtue: the system prompt is built from Hibernate's metamodel, automatically —
    // and the serializer chooses exactly which entities/fields the model gets to see.
    this.metamodelJson = metamodelSerializer.toString(metamodel);
    this.metamodelPrompt = SystemMessage.from(METAMODEL_PROMPT.formatted(metamodelJson));
    log.debug("Metamodel prompt:\n{}", metamodelPrompt.text());
    chatMemory.add(metamodelPrompt);
  }

  /** The exact metamodel view handed to the LLM (after any hiding). Useful to prove what it can see. */
  public String describeMetamodel() {
    return metamodelJson;
  }

  @Override
  public void clear() {
    chatMemory.clear();
    chatMemory.add(metamodelPrompt);
  }

  // ── NL question -> validated SelectionQuery ────────────────────────────────

  @Override
  public <T> SelectionQuery<T> createAiQuery(String message, SharedSessionContract session, Class<T> resultType) {
    final String hql = extractHql(generateHql(message, resultType));
    log.info("Generated HQL: {}", hql);
    return toSelectionQuery(session, hql, resultType);
  }

  private String generateHql(String message, Class<?> resultType) {
    final ManagedDomainType<?> managedType =
        resultType != null && resultType != Object.class && !resultType.isInterface()
            ? metamodel.findManagedType(resultType)
            : null;
    final String userText = managedType == null
        ? message
        : message + "\nThe query must return objects of type \"" + managedType.getTypeName() + "\".";

    chatMemory.add(UserMessage.from(userText));

    final ChatRequest request = ChatRequest.builder()
        .messages(chatMemory.messages())
        .responseFormat(hqlResponseFormat()) // enforce {"hql": "..."} when the model supports it
        .build();

    final ChatResponse response = chatModel.chat(request);
    return response.aiMessage().text();
  }

  @SuppressWarnings("unchecked")
  private static <T> SelectionQuery<T> toSelectionQuery(SharedSessionContract session, String hql, Class<T> resultType) {
    final Class<T> effective = resultType != null ? resultType : (Class<T>) (Class<?>) Object.class;
    return session.createSelectionQuery(hql, effective);
  }

  // ── Execute + let the LLM phrase the answer from real data ─────────────────

  @Override
  public String executeQuery(String message, SharedSessionContract session) {
    final SelectionQuery<?> query = createAiQueryWithRetry(message, session);
    return executeQuery(query, session);
  }

  @Override
  public String executeQuery(SelectionQuery<?> query, SharedSessionContract session) {
    final String json;
    try {
      json = executeQueryToJson(query, session);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    final String prompt = "The query returned the following data (in JSON format):\n" + json
        + "\nBased on the data above, answer the original question in plain natural language. "
        + "Do not create a query or suggest further steps to take!";

    chatMemory.add(UserMessage.from(prompt));

    final ChatResponse response = chatModel.chat(ChatRequest.builder()
        .messages(chatMemory.messages())
        .build());
    return response.aiMessage().text();
  }

  /**
   * Serializes the query results to JSON using Hibernate's own mapping-aware serializer
   * from the official library. Public so the controller can expose the raw data path.
   */
  public <T> String executeQueryToJson(SelectionQuery<T> query, SharedSessionContract session) throws IOException {
    final List<? extends T> results = query.getResultList();
    final SessionFactoryImplementor factory = (SessionFactoryImplementor) session.getFactory();
    // Hibernate's own serializer renders every mapped attribute (including hidden ones on a
    // whole-entity SELECT), so wrap it to strip the hidden fields from the JSON.
    final ResultsSerializer serializer =
        new FilteringResultsSerializer(new ResultsJsonSerializerImpl(factory), hiddenResultFields);
    return serializer.toString(results, query);
  }

  /**
   * Builds the query, and if Hibernate rejects the generated HQL, feeds the parse error
   * back to the model and asks for a correction. This is the self-correcting behaviour of
   * the reference demo's content retriever, kept deliberately simple.
   */
  private SelectionQuery<?> createAiQueryWithRetry(String message, SharedSessionContract session) {
    RuntimeException last = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      final String hql = extractHql(generateHql(message, null));
      log.info("Attempt {}/{} — generated HQL: {}", attempt, MAX_ATTEMPTS, hql);
      try {
        return session.createSelectionQuery(hql, Object.class);
      } catch (RuntimeException e) {
        last = e;
        log.warn("Hibernate rejected the HQL (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
        chatMemory.add(UserMessage.from(
            "The HQL query you generated:\n" + hql
                + "\nwas rejected by Hibernate with this error:\n" + e.getMessage()
                + "\nGenerate a corrected HQL SELECT query for the original request: " + message));
      }
    }
    throw last;
  }

  // ── HQL extraction (dependency-free; works with or without structured output) ──

  static String extractHql(String response) {
    if (response == null || response.isBlank()) {
      throw new IllegalArgumentException("Empty model response");
    }
    String hql = extractJsonStringField(response, "hql");
    if (hql == null || hql.isBlank()) {
      hql = extractSelect(response);
    }
    if (hql == null || hql.isBlank()) {
      throw new IllegalArgumentException("Could not extract an HQL query from model response: " + response);
    }
    return hql.trim();
  }

  private static String extractSelect(String response) {
    final Matcher matcher = HQL_PATTERN.matcher(response);
    return matcher.find() ? matcher.group().trim() : null;
  }

  /**
   * Minimal JSON string-field reader for {@code {"hql":"..."}}. Avoids depending on a
   * specific Jackson major version (Spring Boot 4 ships Jackson 3 by default).
   */
  private static String extractJsonStringField(String json, String field) {
    final String key = "\"" + field + "\"";
    final int k = json.indexOf(key);
    if (k < 0) {
      return null;
    }
    int i = json.indexOf(':', k + key.length());
    if (i < 0) {
      return null;
    }
    i++;
    while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
      i++;
    }
    if (i >= json.length() || json.charAt(i) != '"') {
      return null;
    }
    i++; // opening quote
    final StringBuilder sb = new StringBuilder();
    while (i < json.length()) {
      final char c = json.charAt(i);
      if (c == '\\' && i + 1 < json.length()) {
        final char n = json.charAt(i + 1);
        switch (n) {
          case '"' -> sb.append('"');
          case '\\' -> sb.append('\\');
          case '/' -> sb.append('/');
          case 'n' -> sb.append('\n');
          case 't' -> sb.append('\t');
          case 'r' -> sb.append('\r');
          default -> sb.append(n);
        }
        i += 2;
      } else if (c == '"') {
        return sb.toString();
      } else {
        sb.append(c);
        i++;
      }
    }
    return null;
  }

  private static ResponseFormat hqlResponseFormat() {
    return ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("HQL")
            .rootElement(JsonObjectSchema.builder()
                .addStringProperty("hql")
                .required("hql")
                .build())
            .build())
        .build();
  }
}
