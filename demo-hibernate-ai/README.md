# Hibernate AI Demo - JCON 2026

Natural language data access in Java with **Spring Boot**, **LangChain4j**, **Hibernate**, **H2**, **Ollama**, and optional **MCP**.

Inspired by Marco Belladelli's *"Talk to Your Data"* session at JCON 2026.

---

## What This Demo Shows

Instead of manually writing SQL or HQL, the user asks questions in natural language:

```text
What are the top 5 most expensive products?
How many items are out of stock?
Which categories have more than 5 products?
```

The runtime flow is:

```text
Question -> @AiService generates HQL -> Hibernate validates and executes -> @AiService formats the real result
```

The LLM does not execute SQL directly. Hibernate executes read-only HQL against the Java entity model, so the demo keeps query execution type-aware, portable, and safer than free-form SQL.

The LangChain4j integration follows the Spring Boot starter approach:

- `langchain4j-ollama-spring-boot-starter` creates the `ollamaChatModel` bean from `application.properties`.
- `langchain4j-spring-boot-starter` scans interfaces annotated with `@AiService`.
- `HqlGenerator` and `AnswerFormatter` are declarative AI Services wired to the `ollamaChatModel`.
- `DatabaseAssistant` is a normal Spring service that coordinates the AI Services and the Hibernate tool.

---

## Prerequisites

| Tool | Minimum version |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Docker + Docker Compose | Any recent version |

This repository does not include a Maven wrapper, so use `mvn`, not `./mvnw`.

---

## Full Setup And Test Flow

### 1. Open the project

```bash
cd demo-hibernate-ai
```

### 2. Start Ollama with Docker Compose

```bash
docker compose up -d ollama
```

### 3. Pull the local model

The application is configured to use `qwen2.5:3b`.

```bash
docker compose exec ollama ollama pull qwen2.5:3b
```

Check that the model is installed:

```bash
docker compose exec ollama ollama list
```

You should see `qwen2.5:3b` in the output.

### 4. Start the Spring Boot application

```bash
mvn spring-boot:run
```

The app starts at:

```text
http://localhost:8080
```

The H2 database is in-memory and is automatically populated from `src/main/resources/data.sql` with 25 products across 5 categories.

### 5. Test the REST assistant

In another terminal:

```bash
curl "http://localhost:8080/assistant/ask?q=How+many+products+do+we+have?"
```

Expected answer:

```text
We have 25 products.
```

### 6. Verify the database directly in H2

Open:

```text
http://localhost:8080/h2-console
```

Use:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:demodb` |
| User Name | `sa` |
| Password | leave empty |

Run:

```sql
SELECT COUNT(*) FROM PRODUCTS;
```

Expected result:

```text
25
```

---

## Demo Questions

```bash
# Simple count
curl "http://localhost:8080/assistant/ask?q=How+many+products+do+we+have?"

# Top N
curl "http://localhost:8080/assistant/ask?q=What+are+the+top+5+most+expensive+products?"

# Category filter
curl "http://localhost:8080/assistant/ask?q=List+all+products+in+the+Electronics+category"

# Aggregation
curl "http://localhost:8080/assistant/ask?q=What+is+the+average+price+per+category?"

# Low stock
curl "http://localhost:8080/assistant/ask?q=Which+products+have+less+than+10+items+in+stock?"

# Write attempt, expected to be refused or blocked
curl "http://localhost:8080/assistant/ask?q=Delete+all+products+cheaper+than+10+euros"
```
Restricted entity fields are blocked even though they exist in the `Product`
entity and backing table:

```bash
# costPrice
curl "http://localhost:8080/assistant/ask?q=What+is+the+cost+price+of+each+product?"
curl "http://localhost:8080/assistant/ask?q=Which+product+has+the+highest+cost+price?"
curl "http://localhost:8080/assistant/ask?q=Show+me+the+margin+between+price+and+cost+price"
# supplierCode
curl "http://localhost:8080/assistant/ask?q=Show+me+the+supplier+code+for+all+products"
curl "http://localhost:8080/assistant/ask?q=List+products+with+their+supplier+codes"
```

Expected responses:

```text
ERROR: Field 'costPrice' is not accessible via this assistant.
ERROR: Field 'supplierCode' is not accessible via this assistant.
```

---

## Useful URLs

| URL | Purpose |
|---|---|
| `http://localhost:8080/assistant/ask?q=...` | Ask natural-language questions |
| `http://localhost:8080/h2-console` | Inspect the in-memory H2 database |

---

## Project Structure

```text
src/main/java/com/demo/
├── DemoApplication.java
├── model/
│   ├── Category.java            JPA entity for product categories
│   └── Product.java             JPA entity for products
├── tool/
│   └── HibernateQueryTool.java  Executes read-only HQL through Hibernate
├── assistant/
│   ├── HqlGenerator.java        LangChain4j @AiService that converts questions to HQL
│   ├── AnswerFormatter.java     LangChain4j @AiService that formats real DB results
│   └── DatabaseAssistant.java   Coordinates AI Services, HQL execution, and demo-safe rules
├── controller/
│   └── AssistantController.java REST API: GET and POST /assistant/ask
└── config/
    └── McpServerConfig.java     Optional MCP server setup placeholder

src/main/resources/
├── application.properties       Ollama, H2, and JPA configuration
└── data.sql                     Sample data: 25 products in 5 categories
```

---

## How The Assistant Avoids Fake Results

Local models can sometimes answer from memory instead of making a tool call. This demo avoids that by using a controlled two-step flow:

1. `HqlGenerator`, a LangChain4j `@AiService`, converts the user question into one read-only HQL query.
2. `HibernateQueryTool` executes that HQL through `EntityManager`.
3. `AnswerFormatter`, another `@AiService`, receives the real database result and only formats the final answer.

Every read-only question goes through the `HqlGenerator`. There are no hardcoded HQL shortcuts for the main demo questions, so the presentation shows the real AI-assisted query generation path.

This is why the count endpoint returns the same value as H2:

```text
25
```

---

## Troubleshooting

### `model '...' not found`

Install the configured model:

```bash
docker compose exec ollama ollama pull qwen2.5:3b
```

Then restart the Spring Boot app.

### `Table "CATEGORIES" not found`

The project uses:

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.defer-datasource-initialization=true
```

This makes Hibernate create the tables before Spring Boot runs `data.sql`.

### Port 8080 is already in use

Stop the existing process or run the app on another port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Then test:

```bash
curl "http://localhost:8081/assistant/ask?q=How+many+products+do+we+have?"
```

### Ollama is not reachable

Check the container:

```bash
docker compose ps
```

The app expects Ollama at:

```properties
langchain4j.ollama.chat-model.base-url=http://localhost:11434
```

---

## Switching Models

Pull another Ollama model:

```bash
docker compose exec ollama ollama pull mistral
```

Change:

```properties
langchain4j.ollama.chat-model.model-name=mistral
```

Then restart the app.

---

## Optional: Use OpenAI Instead Of Ollama

Add the OpenAI starter:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
</dependency>
```

Then configure:

```properties
langchain4j.open-ai.chat-model.api-key=${OPENAI_API_KEY}
langchain4j.open-ai.chat-model.model-name=gpt-4o-mini
langchain4j.open-ai.chat-model.temperature=0.0
```

Comment out or remove the Ollama chat model properties when using OpenAI.

---

## Optional: Expose As An MCP Tool

To expose the assistant through MCP:

1. Uncomment the MCP dependency in `pom.xml`.
2. Uncomment the implementation in `McpServerConfig.java`.
3. Restart the application.

The MCP server will be available at:

```text
http://localhost:8080/mcp/sse
```

Claude Desktop example:

```json
{
  "mcpServers": {
    "product-db": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

After restarting Claude Desktop, the `query_product_database` tool should be available.

---

## References

- [Marco Belladelli - mbellade/demos Hibernate Assistant](https://github.com/mbellade/demos/tree/main/quarkus/hibernate-assistant)
- [LangChain4j Spring Boot Starter](https://docs.langchain4j.dev/tutorials/spring-boot-integration)
- [Ollama models](https://ollama.com/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [JCON Europe 2026](https://jcon.one/)
