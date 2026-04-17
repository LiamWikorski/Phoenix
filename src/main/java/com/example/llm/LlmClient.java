package com.example.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final LlmProperties properties;

    public LlmClient(LlmProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public LlmResponse generate(String contextJson) {
        try {
            String body = buildRequestBody(contextJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            logPayload("prompt", body);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logPayload("response", response.body());

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("LLM request failed: status=" + response.statusCode());
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM call failed", e);
        }
    }

    private String buildRequestBody(String contextJson) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", properties.getModel());

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode system = mapper.createObjectNode();
        system.put("role", "system");
        system.put("content", LlmPromptTemplates.SYSTEM_PROMPT);
        messages.add(system);

        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", LlmPromptTemplates.USER_PROMPT_TEMPLATE.formatted(contextJson));
        messages.add(user);

        root.set("messages", messages);
        root.put("temperature", 0.2);
        ObjectNode responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_object");
        root.set("response_format", responseFormat);
        return mapper.writeValueAsString(root);
    }

    private LlmResponse parseResponse(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode messageContent = root.path("choices").path(0).path("message").path("content");
        if (messageContent.isMissingNode()) {
            throw new IllegalStateException("LLM response missing content");
        }
        JsonNode parsed = mapper.readTree(messageContent.asText());
        return mapper.treeToValue(parsed, LlmResponse.class);
    }

    private void logPayload(String type, String payload) {
        Map<String, Object> logEntry = Map.of(
                "ts", Instant.now().toString(),
                "type", type,
                "payload", payload
        );
        try {
            log.info("LLM {}: {}", type, mapper.writeValueAsString(logEntry));
        } catch (Exception ex) {
            log.info("LLM {}: {}", type, payload);
        }
    }
}
