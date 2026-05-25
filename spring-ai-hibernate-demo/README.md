# Spring AI Hibernate Demo - JCON 2026

Natural language data access in Java with **Spring Boot**, **Spring AI**, **Hibernate/JPA**, **H2**, **Ollama**, and **Lombok**.

This is the Spring AI version of the LangChain4j demo in `../demo-hibernate-ai`.

## What It Shows

The architecture is intentionally controlled:

```text
Question
  -> Spring AI ChatClient generates HQL
  -> Hibernate validates and executes read-only HQL
  -> Spring AI ChatClient formats the real database result
```

The Spring AI version deliberately sends every read-only question through the LLM for HQL generation. Hibernate remains the execution boundary, so the LLM never touches SQL or the database directly.

## Run

```bash
cd local-llm/spring-ai-hibernate-demo

# Option A: local Ollama, best on Mac/Apple Silicon
ollama pull qwen2.5:3b
ollama serve

# Option B: containerized Ollama
docker compose up -d ollama
docker compose exec ollama ollama pull qwen2.5:3b

mvn spring-boot:run
```

This project includes `.mvn/maven.config`, so Maven automatically uses `maven-central-settings.xml` and forces dependency updates. That avoids cached failures from an unreachable corporate mirror.

If you want to run the same command explicitly, use:

```bash
mvn -s maven-central-settings.xml spring-boot:run
```

The app starts at:

```text
http://localhost:8080
```

## Test Questions

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
# unknown entity
curl "http://localhost:8080/assistant/ask?q=List+each+user+name"
```
Expected responses:

```text
ERROR: Field 'costPrice' from entity 'Product' is not accessible via this assistant.
ERROR: Field 'supplierCode' from entity 'Product' is not accessible via this assistant.
ERROR: Entity 'User' is not accessible via this assistant. Allowed entities: [Product, Category]
```

## Project Structure

```text
src/main/java/com/demo/
├── DemoApplication.java
├── model/
│   ├── Category.java            JPA entity for product categories
│   └── Product.java             JPA entity for products
├── tool/
│   ├── HibernateQueryTool.java  Executes read-only HQL through Hibernate
│   ├── HqlAccessValidator.java  Validates allowed entities, fields, and read-only HQL
│   └── HqlResultFormatter.java  Formats raw Hibernate results for the answer formatter
├── assistant/
│   ├── HqlGenerator.java        Spring AI service that converts questions to HQL
│   ├── AnswerFormatter.java     Spring AI service that formats real DB results
│   └── DatabaseAssistant.java   Coordinates Spring AI services and HQL execution
└── controller/
    └── AssistantController.java REST API: GET and POST /assistant/ask

src/main/resources/
├── application.properties       Ollama, H2, and JPA configuration
└── data.sql                     Sample data: 25 products in 5 categories
```

## H2 Console

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:demodb
User: sa
Password: <empty>
```

## Spring AI Pieces

- `spring-ai-starter-model-ollama` auto-configures Ollama.
- `spring.ai.ollama.base-url` points at `http://localhost:11434`.
- `spring.ai.ollama.chat.options.model=qwen2.5:3b`.
- `DatabaseAssistant` injects `ChatClient.Builder` and builds a `ChatClient`.
- `ChatClient.prompt().system(...).user(...).call().content()` replaces the LangChain4j `@AiService` interfaces from the other demo.

## MCP Support

This project does **not** include MCP (Model Context Protocol) support. The Spring AI version focuses on the core ChatClient API and the two-step orchestration pipeline (HQL generation → HQL execution → answer formatting).

If you want to expose the assistant as an MCP tool for Claude Desktop or other MCP clients, use the LangChain4j version in `../demo-hibernate-ai`. That project includes an optional `McpServerConfig.java` and the necessary dependency — both are commented out and ready to activate.

## References

- Spring AI Ollama docs: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
- Spring AI ChatClient docs: https://docs.spring.io/spring-ai/reference/api/chatclient.html
