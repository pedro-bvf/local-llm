package com.demo.config;

// ─────────────────────────────────────────────────────────────────────────────
// DEMO BLOCK 4 - Expose the assistant as an MCP tool
// ─────────────────────────────────────────────────────────────────────────────
//
// PREREQUISITE: Uncomment the dependency in pom.xml:
//
//   <dependency>
//       <groupId>io.modelcontextprotocol.sdk</groupId>
//       <artifactId>mcp-spring-webmvc</artifactId>
//       <version>0.9.0</version>
//   </dependency>
//
// Then uncomment the full implementation in this class.
// The MCP server will be available at: http://localhost:8080/mcp/sse
//
// Claude Desktop configuration (~/.config/claude/claude_desktop_config.json):
// {
//   "mcpServers": {
//     "product-db": {
//       "url": "http://localhost:8080/mcp/sse"
//     }
//   }
// }
// ─────────────────────────────────────────────────────────────────────────────

/*
import com.demo.assistant.DatabaseAssistant;
import io.modelcontextprotocol.sdk.McpSyncServer;
import io.modelcontextprotocol.sdk.McpSyncServerTransportProvider;
import io.modelcontextprotocol.sdk.schema.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class McpServerConfig {

    @Bean
    public McpSyncServer mcpServer(
            McpSyncServerTransportProvider transportProvider,
            DatabaseAssistant assistant) {

        return McpSyncServer.using(transportProvider)
                .serverInfo("hibernate-ai-demo", "1.0.0")
                .tool(
                        McpSchema.Tool.builder()
                                .name("query_product_database")
                                .description("""
                                        Query the product management database using natural language.
                                        Ask questions like:
                                        - 'How many products are in stock?'
                                        - 'What are the top 5 most expensive products?'
                                        - 'Which categories have less than 10 products?'
                                        """)
                                .inputSchema(McpSchema.JsonSchema.builder()
                                        .type("object")
                                        .properties(Map.of(
                                                "question", McpSchema.JsonSchema.builder()
                                                        .type("string")
                                                        .description("Natural language question about the product database")
                                                        .build()
                                        ))
                                        .required(List.of("question"))
                                        .build())
                                .build(),
                        args -> {
                            String question = (String) args.get("question");
                            String answer   = assistant.chat(question);
                            return new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent(answer)),
                                    false
                            );
                        }
                )
                .build();
    }
}
*/

public class McpServerConfig {
    // Placeholder - see the comments above to enable the MCP server.
}
