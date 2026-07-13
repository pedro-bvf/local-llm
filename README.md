# Local LLM Database Assistant - JCON 2026

Demonstrações de acesso a dados em linguagem natural usando Java, Hibernate,
modelos locais através do Ollama e diferentes frameworks de integração com LLMs.

## Versões

### V1 - Implementação manual

A V1 implementa manualmente o fluxo de geração, validação e execução de HQL.

| Projeto | Integração LLM |
|---|---|
| [demo-hibernate-ai](v1/demo-hibernate-ai/) | LangChain4j |
| [spring-ai-hibernate-demo](v1/spring-ai-hibernate-demo/) | Spring AI |

### V2 - Hibernate Assistant

A V2 utiliza a biblioteca oficial `org.hibernate.orm:hibernate-assistant` para
descrever o metamodelo, validar consultas HQL e serializar os resultados.

| Projeto | Integração LLM |
|---|---|
| [hibernate-assistant-demo](v2/hibernate-assistant-demo/) | LangChain4j |
| [spring-ai-assistant-demo](v2/spring-ai-assistant-demo/) | Spring AI |

## Fluxo

```text
Pergunta em linguagem natural
  -> LLM gera HQL
  -> Hibernate valida e executa a consulta
  -> resultados são enviados ao LLM
  -> resposta em linguagem natural
```

## Requisitos

- Java 21
- Maven 3.9+
- Ollama
- Docker e Docker Compose, opcionalmente

As instruções de instalação e execução estão no README de cada projeto.

## Documentação

- [Documentação em português](v1/local_llm_pt.docx)
- [Documentação em espanhol](v1/local_llm_es.docx)
- [Revisão da documentação em espanhol](v1/local_llm_es_review.docx)
- [Diagrama do assistente de base de dados](v1/database_assistant.drawio)
- [Diagrama do Ollama](v1/ollama.drawio)
- [Vídeo da apresentação](v1/local-llm-video.mp4)
