package com.example.examplemod.ollama;

import com.example.examplemod.Config;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {
    private static final String GENERATE_PATH = "/api/generate";

    private final HttpClient httpClient;
    private final URI baseUri;
    private final String model;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration retryDelay;

    public static OllamaClient fromConfig() {
        return new OllamaClient(
            Config.ollamaBaseUrl,
            Config.ollamaModel,
            Config.requestTimeoutMs,
            Config.ollamaMaxRetries,
            Config.ollamaRetryDelayMs
        );
    }

    public OllamaClient(String baseUrl, String model, int timeoutMs, int maxRetries, int retryDelayMs) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
        this.baseUri = URI.create(baseUrl);
        this.model = model;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.maxRetries = Math.max(0, maxRetries);
        this.retryDelay = Duration.ofMillis(Math.max(0, retryDelayMs));
    }

    public String generateResponse(String prompt) throws IOException, InterruptedException {
        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return sendGenerateRequest(prompt);
            } catch (IOException exception) {
                lastException = exception;
            }
            if (attempt < maxRetries) {
                Thread.sleep(retryDelay.toMillis());
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new IOException("Ollama request failed with unknown error.");
    }

    private String sendGenerateRequest(String prompt) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(baseUri.resolve(GENERATE_PATH))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama request failed with status " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (json.has("response")) {
            return json.get("response").getAsString();
        }
        throw new IOException("Ollama response missing 'response' field.");
    }
}
