# Hibernate Assistant Demo — JCON 2026

Natural-language data access in Java using the **real** Hibernate Assistant library
(`org.hibernate.orm:hibernate-assistant`), with **Spring Boot 4.1**, **Hibernate ORM 7.4**,
**LangChain4j**, **H2** and **Ollama**.

This is the *"done with the actual library"* counterpart to the two V1 demos
([`demo-hibernate-ai`](../../v1/demo-hibernate-ai),
[`spring-ai-hibernate-demo`](../../v1/spring-ai-hibernate-demo)), which only **reimplemented the idea**
by hand. See [How this differs](#how-this-differs-from-the-hand-rolled-demos).

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

Our `HibernateAssistantLC4J` only contributes the LLM glue (chat call, JSON-schema response,
chat memory, a self-correcting retry). It implements the library interface
`org.hibernate.tool.language.HibernateAssistant`.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Ollama | running locally (native `ollama serve`, or the optional `docker-compose.yml`) |

Pull a model (a *coder* model handles HQL best):

```bash
ollama pull qwen2.5-coder:7b      # default in application.properties
# alternatives you may already have: qwen2.5-coder:3b (faster), qwen3:8b
```

The app talks to `http://localhost:11434` either way. To use a different model:

```properties
# src/main/resources/application.properties
assistant.ollama.model=qwen2.5-coder:3b
```

> Behind a corporate Maven mirror? This builds fine as long as the mirror proxies Maven
> Central (Spring Boot 4.1, Hibernate 7.4.1 and `hibernate-assistant` 7.4.1 all live there).
> Otherwise point Maven at Central with `-s` and a settings file, as in the sibling demo.

---

## Run

```bash
cd hibernate-assistant-demo
mvn spring-boot:run
```

H2 is in-memory and seeded from `data.sql` (5 categories, 6 suppliers across 6 countries,
25 products). App starts at `http://localhost:8080`.

---

## Try it

The quickest way is the bundled script (app must be running):

```bash
./demo.sh         # natural-language answers
./demo.sh hql     # show the HQL the model generated for each question + raw JSON
```

Or call the endpoints directly. A curated set, grouped by the capability it shows:

```bash
B=http://localhost:8080/assistant

# Counts & aggregation
curl "$B/ask?q=How+many+products+do+we+have?"
curl "$B/ask?q=What+is+the+average+price+per+category?"
curl "$B/ask?q=What+is+the+total+stock+value+(price+times+stock)+across+all+products?"

# Sorting / top-N
curl "$B/ask?q=What+are+the+top+5+most+expensive+products?"
curl "$B/ask?q=Which+3+products+have+the+lowest+stock?"

# Relationship traversal (category / supplier)
curl "$B/ask?q=List+all+products+in+the+Electronics+category"
curl "$B/ask?q=How+many+products+does+each+supplier+provide?"
curl "$B/ask?q=Which+products+are+supplied+by+Apple+Inc.?"

# Embeddable (supplier.address)
curl "$B/ask?q=Which+suppliers+are+in+Italy?"
curl "$B/ask?q=In+which+cities+are+our+suppliers+located?"

# Multi-hop filters (relationship + embeddable in one query)
curl "$B/ask?q=List+products+from+suppliers+in+Switzerland"
curl "$B/ask?q=How+many+products+come+from+suppliers+outside+the+USA?"
curl "$B/ask?q=What+is+the+average+product+price+per+supplier+country?"

# Write attempt — the read-only SELECT path means Hibernate simply won't accept it
curl "$B/ask?q=Delete+all+products+cheaper+than+10+euros"

# See the generated HQL + the raw JSON Hibernate Assistant produced
curl "$B/hql?q=list+products+from+suppliers+in+Switzerland"

# Manually reset the conversation (each /ask and /hql already starts fresh on its own)
curl "$B/clear"
```

> Each `/ask` and `/hql` call resets the chat context first, so independent questions are
> reproducible and don't contaminate each other. The shared `ChatMemory` (and `/clear`) are
> kept because multi-turn follow-ups are a natural extension — drop the per-request reset in
> `AssistantController` to enable them.

The `/hql` endpoint is the convincing one for a technical audience: it prints the actual
HQL the model produced (e.g. `select p from Product p join p.supplier s where s.address.country = 'Italy'`)
— note it queries the **embeddable** `address.country` and the **relationship** `p.supplier`,
which the LLM only knows because the metamodel told it.

Cross-check against the database directly at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:demodb`, user `sa`, empty password).

---

## How this differs from the hand-rolled demos

| | `demo-hibernate-ai` / `spring-ai-hibernate-demo` | **this demo** |
|---|---|---|
| Schema given to the LLM | **Hard-coded** in the system prompt (`"Product(id, name, price, stock, category)"`) | **Generated from the metamodel** by the library |
| HQL validation | **Hand-rolled regex** whitelist of entities/fields | **Hibernate parses the HQL** (`createSelectionQuery`) |
| Result formatting | Custom `instanceof` formatter | Mapping-aware `ResultsJsonSerializer` (relationships, embeddables) |
| Adding a field | Edit entity **+ prompt + validator** (3 places) | Edit the entity **only** (metamodel updates itself) |
| Wrong-but-valid HQL | Silently returned | Same risk, but **self-corrects**: Hibernate's parse error is fed back and the model retries |
| Hiding fields/tables from the LLM | Regex blocklist after generation | Custom `MetamodelSerializer` — the model is never told they exist |
| Uses Hibernate Assistant | **No** | **Yes** (`org.hibernate.orm:hibernate-assistant`) |

## Hiding tables and fields from the LLM (vs a direct-DB MCP)

A big argument for Hibernate Assistant over an **MCP wired straight to the database**: that MCP sees
*every* table and column, so the model can read anything the DB user can. Here, the assistant is
primed **only** with what a `MetamodelSerializer` emits — the control lives in your domain layer.

Hibernate Assistant ships one serializer (`MetamodelJsonSerializerImpl`) that exposes the whole
mapped model and has no hide option. To curate the view you implement its SPI
`org.hibernate.tool.language.spi.MetamodelSerializer`. This demo's
[`FilteringMetamodelSerializer`](src/main/java/com/demo/assistant/FilteringMetamodelSerializer.java)
**reuses Hibernate's own serializer for the format** and just strips the hidden parts:

```java
// AssistantConfig
new FilteringMetamodelSerializer(
    Set.of("AuditLog"),                       // hide a whole table/entity
    Map.of("Product", Set.of("costPrice")));  // hide a sensitive field
```

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

# The model can't surface what it doesn't know about. What you'll observe:
#  - cost price: /hql shows it queries p.price (the visible field), NEVER p.costPrice —
#    the real cost data never leaves the DB.
curl "http://localhost:8080/assistant/hql?q=What+is+the+cost+price+of+each+product?"
#  - audit log: the model doesn't know the table exists, so it guesses a name that
#    Hibernate refuses to resolve — no rows come back.
curl "http://localhost:8080/assistant/ask?q=Show+me+the+audit+log+entries"
```

> **Scope, stated honestly.** Prompt + result filtering covers the queries the assistant actually
> produces (it doesn't know the hidden names, and whole-entity selects are scrubbed). The columns
> remain mapped, so a caller who *hand-writes* HQL explicitly projecting a hidden scalar
> (`SELECT p.costPrice FROM Product p`) is the one remaining edge — for that you'd add a check on the
> parsed query (SQM). The point versus a raw-DB MCP stands: the exposed surface is decided in your
> code, not inherited wholesale from the database schema.

---

## References

- Hibernate Assistant (in Hibernate ORM): https://hibernate.org/orm/
- Reference demo (Quarkus): https://github.com/mbellade/demos/tree/main/quarkus/hibernate-assistant
- LangChain4j: https://docs.langchain4j.dev/
- Talk: *Talk to Your Data* — Marco Belladelli, JCON 2026
