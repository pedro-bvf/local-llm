# Spring AI Assistant Demo — JCON 2026

Natural-language data access in Java using the **real** Hibernate Assistant library
(`org.hibernate.orm:hibernate-assistant`), with **Spring Boot 4.1**, **Hibernate ORM 7.4**,
**Spring AI 2.0**, **H2** and **Ollama**.

This is the **Spring AI** twin of [`../hibernate-assistant-demo`](../hibernate-assistant-demo),
which wires the same library to **LangChain4j**. Same pattern, same security layers, same tests —
only the LLM glue differs. It exists to prove that the parts that matter are independent of the
LLM framework. See [How this differs](#how-this-differs).

---

## What it shows

```
Question
  → metamodel JSON (built by Hibernate Assistant from the JPA mapping) primes the LLM
  → LLM returns one HQL SELECT
  → Hibernate parses & validates the HQL  (session.createSelectionQuery)
  → Hibernate Assistant serializes the real results to JSON (mapping-aware, no circularity)
  → LLM phrases the answer from that data
```

The two pieces that matter come **straight from the official artifact**, not from our code:

| Concern | Provided by | Class |
|---|---|---|
| Describe the schema to the LLM | `hibernate-assistant` | `MetamodelJsonSerializerImpl` |
| Validate the generated HQL | Hibernate ORM | `SharedSessionContract.createSelectionQuery` |
| Serialize results for the LLM | `hibernate-assistant` | `ResultsJsonSerializerImpl` |

Our `HibernateAssistantSpringAI` only contributes the LLM glue (chat call, JSON-schema response
via Ollama's `outputSchema`, chat memory, a self-correcting retry). It implements the library
interface `org.hibernate.tool.language.HibernateAssistant`.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Ollama | running locally (native `ollama serve`, or the optional `docker-compose.yml`) |

Start Ollama and pull a model (a *coder* model handles HQL best):

```bash
# Native Ollama
ollama pull qwen2.5-coder:7b      # default in application.properties

# Or Docker Compose (no local Ollama CLI required)
docker compose up -d ollama
docker compose exec ollama ollama pull qwen2.5-coder:7b
```

Alternatives you may already have include `qwen2.5-coder:3b` (faster) and `qwen3:8b`.

The app talks to `http://localhost:11434` either way. To use a different model:

```properties
# src/main/resources/application.properties
spring.ai.ollama.chat.model=qwen2.5-coder:3b
```

> Behind a corporate Maven mirror? This builds fine as long as the mirror proxies Maven
> Central (Spring Boot 4.1, Hibernate 7.4.1, `hibernate-assistant` 7.4.1 and Spring AI 2.0.0
> all live there).

---

## Run

```bash
cd spring-ai-assistant-demo
mvn spring-boot:run
```

H2 is in-memory and seeded from `data.sql` (5 categories, 6 suppliers across 6 countries,
25 products). App starts at `http://localhost:8080`.

---

## Try it

Call the endpoints directly. A curated set, grouped by the capability it shows:

```bash
B=http://localhost:8080/assistant

# Counts & aggregation
curl "$B/ask?q=How+many+products+do+we+have?"
curl "$B/ask?q=What+is+the+average+price+per+category?"

# Sorting / top-N
curl "$B/ask?q=What+are+the+top+5+most+expensive+products?"

# Relationship traversal (category / supplier)
curl "$B/ask?q=List+all+products+in+the+Electronics+category"
curl "$B/ask?q=Which+products+are+supplied+by+Apple+Inc.?"

# Embeddable (supplier.address) + relationship in one query
curl "$B/ask?q=Which+products+are+supplied+by+companies+in+Switzerland?"
curl "$B/ask?q=Which+suppliers+are+in+Italy?"

# Write attempt — the read-only SELECT path means Hibernate simply won't accept it
curl "$B/ask?q=Delete+all+products+cheaper+than+10+euros"

# See the generated HQL + the raw JSON Hibernate Assistant produced
curl "$B/hql?q=Which+products+are+supplied+by+companies+in+Switzerland?"

# The exact metamodel the LLM receives — note: no costPrice, no AuditLog
curl "$B/metamodel"

# Hidden field — the model is never told costPrice exists
curl "$B/ask?q=What+is+the+cost+price+of+each+product?"

# Hidden entity — AuditLog never appears in the prompt
curl "$B/ask?q=Show+me+the+audit+log+entries"

# Manually reset the conversation (each /ask and /hql already starts fresh on its own)
curl "$B/clear"
```

> Each `/ask` and `/hql` call resets the chat context first, so independent questions are
> reproducible and don't contaminate each other. The shared `ChatMemory` (and `/clear`) are
> kept because multi-turn follow-ups are a natural extension — drop the per-request reset in
> `AssistantController` to enable them.

Cross-check against the database directly at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:demodb`, user `sa`, empty password).

---

## How this differs

Three demos, one library. The first two **reimplemented** the idea by hand; the last two **use**
the official `hibernate-assistant`. This one and `hibernate-assistant-demo` differ *only* in the
LLM glue:

| | `demo-hibernate-ai` / `spring-ai-hibernate-demo` (hand-rolled) | `hibernate-assistant-demo` (LangChain4j) | **this demo** (Spring AI) |
|---|---|---|---|
| Schema given to the LLM | **Hard-coded** in the system prompt | Generated from the metamodel by the library | Generated from the metamodel by the library |
| HQL validation | Hand-rolled regex whitelist | `createSelectionQuery` (Hibernate parses) | `createSelectionQuery` (Hibernate parses) |
| Result formatting | Custom `instanceof` formatter | Mapping-aware `ResultsJsonSerializer` | Mapping-aware `ResultsJsonSerializer` |
| Hiding fields/tables | Regex blocklist after generation | `FilteringMetamodelSerializer` + `FilteringResultsSerializer` | **same two classes, unchanged** |
| Self-correction | None | Hibernate's parse error fed back, retry | Hibernate's parse error fed back, retry |
| LLM integration | `@AiService` / `ChatClient` | LangChain4j `ChatModel` + `ResponseFormat` | Spring AI `ChatModel` + Ollama `outputSchema` |
| Chat memory | None | LangChain4j `MessageWindowChatMemory` | Spring AI `MessageWindowChatMemory` |
| Structured output | n/a | LangChain4j JSON-schema `ResponseFormat` | `OllamaChatOptions.outputSchema(...)` |

The point: `FilteringMetamodelSerializer`, `FilteringResultsSerializer`, the entities, `data.sql`
and the whole test suite are **copied verbatim** from `hibernate-assistant-demo`. Only
`HibernateAssistantSpringAI`, `AssistantConfig` (injects the auto-configured `ChatModel` instead of
building one), `pom.xml` and `application.properties` change. The security and serialization layers
do not depend on the LLM framework.

## Hiding tables and fields from the LLM (vs a direct-DB MCP)

A big argument for Hibernate Assistant over an **MCP wired straight to the database**: that MCP sees
*every* table and column, so the model can read anything the DB user can. Here, the assistant is
primed **only** with what a `MetamodelSerializer` emits — the control lives in your domain layer.

Hiding has to happen in **two places**, because a normal `SELECT p FROM Product p` returns the whole
entity and Hibernate's result serializer renders every mapped column:

- [`FilteringMetamodelSerializer`](src/main/java/com/demo/assistant/FilteringMetamodelSerializer.java)
  keeps the hidden parts out of the **prompt** (the model never learns they exist), and
- [`FilteringResultsSerializer`](src/main/java/com/demo/assistant/FilteringResultsSerializer.java)
  strips them from the **result JSON** (so a whole-entity `SELECT p` can't leak them either).

`Product.costPrice` and the `AuditLog` table are fully mapped and used by the rest of the app, but
the model is never told they exist and the data never reaches it. See for yourself:

```bash
# The exact metamodel the LLM receives — note: no costPrice, no AuditLog
curl "http://localhost:8080/assistant/metamodel"

# The model can't surface what it doesn't know about.
curl "http://localhost:8080/assistant/hql?q=What+is+the+cost+price+of+each+product?"
curl "http://localhost:8080/assistant/ask?q=Show+me+the+audit+log+entries"
```

> **Scope, stated honestly.** Prompt + result filtering covers the queries the assistant actually
> produces. The columns remain mapped, so a caller who *hand-writes* HQL explicitly projecting a
> hidden scalar (`SELECT p.costPrice FROM Product p`) is the one remaining edge — for that you'd add
> a check on the parsed query (SQM). The point versus a raw-DB MCP stands: the exposed surface is
> decided in your code, not inherited wholesale from the database schema.

---

## References

- Hibernate Assistant (in Hibernate ORM): https://hibernate.org/orm/
- Reference demo (Quarkus): https://github.com/mbellade/demos/tree/main/quarkus/hibernate-assistant
- Spring AI: https://docs.spring.io/spring-ai/reference/
- Talk: *Talk to Your Data* — Marco Belladelli, JCON 2026
