package com.querycanvas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiInsightRefiner {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OpenAiInsightRefiner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String refineInsight(String question, String sql, List<Map<String, Object>> rows) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4.1-mini");
        Map<String, Object> payload = Map.of(
            "model", model,
            "input", """
                Question: %s
                SQL: %s
                Rows: %s
                Write a concise 2 sentence analytics insight.
                """.formatted(question, sql, rows)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode outputText = root.get("output_text");
            return outputText != null && !outputText.isNull() ? outputText.asText() : null;
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
