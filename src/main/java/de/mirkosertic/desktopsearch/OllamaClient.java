package de.mirkosertic.desktopsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class OllamaClient {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;

    public OllamaClient(final ObjectMapper objectMapper, final String baseUrl, final String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * Standard-Konstruktor (localhost, gemma2:2b)
     */
    public OllamaClient() {
        this(new ObjectMapper(), "http://localhost:11434", "gemma3:4b");
    }
    
    /**
     * Detaillierte Analyse mit Relevanz-Score
     */
    public AnalysisResult extractContent(final String context) throws Exception {
        final String prompt = String.format("""
                        Extrahiere sehr kurze Schlagworte aus folgendem Text. Gib nur die Schlagworte als kommaseparierte Liste zurück, sonst nichts. Der Text ist: %s\s
        """, context);

        final String response = generate("", prompt, 100);
        return parseAnalysisResult(response);
    }

    public String highlightRelevance(final String queryString, final String content) throws Exception {
        final String systemPrompt = String.format("""
                        Ein Benutzer hat nach '%s' gesucht. Erstelle eine HTML-Zusammenfassung des folgenden Textes in max. vier Sätzen, warum der Text gut zum Suchergebnis passt.
        """, queryString);

        final String response = generate("", content, 10000);
        return response;
    }


    /**
     * Kern-Methode: Generiert Text mit Ollama
     */
    private String generate(final String systemPrompt, final String prompt, final int maxTokens) throws Exception {
        final Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("system", systemPrompt);
        requestBody.put("stream", false);

        final Map<String, Object> options = new HashMap<>();
        options.put("num_predict", maxTokens);
        options.put("temperature", 0.3);  // Konsistentere Ergebnisse
        requestBody.put("options", options);

        final String jsonBody = objectMapper.writeValueAsString(requestBody);

        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(30))
            .build();

        final HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama Error: " + response.body());
        }

        final JsonNode jsonResponse = objectMapper.readTree(response.body());
        return jsonResponse.get("response").asText().trim();
    }

    /**
     * Parst die strukturierte Antwort
     */
    private AnalysisResult parseAnalysisResult(final String response) {
        final AnalysisResult result = new AnalysisResult();
        
        final String[] lines = response.split("\n");
        for (final String line : lines) {
            if (line.startsWith("SUMMARY:")) {
                result.summary = line.substring(8).trim();
            } else if (line.startsWith("RELEVANCE:")) {
                try {
                    result.relevance = Integer.parseInt(
                        line.substring(10).trim().replaceAll("[^0-9]", "")
                    );
                } catch (final NumberFormatException e) {
                    result.relevance = 50; // Default
                }
            } else if (line.startsWith("KEY_POINTS:")) {
                result.keyPoints = line.substring(11).trim();
            }
        }
        
        return result;
    }

    /**
     * Ergebnis-Container
     */
    public static class AnalysisResult {
        public String summary = "";
        public int relevance = 0;
        public String keyPoints = "";
        
        @Override
        public String toString() {
            return String.format(
                "Summary: %s\nRelevance: %d%%\nKey Points: %s",
                summary, relevance, keyPoints
            );
        }
    }
}