package net.runelite.client.plugins.microbot.bosshelper.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.bosshelper.model.BossPredictionResponse;
import net.runelite.client.plugins.microbot.bosshelper.model.BossState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class AgentServerClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(350);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(400);
    private static final String AUTH_HEADER = "X-Agent-Token";

    private final HttpClient httpClient;
    private final Gson gson;
    private String cachedToken;
    private long lastTokenReadTime = 0;

    public AgentServerClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Sends the boss combat telemetry snapshot asynchronously to the Agent Server.
     *
     * @param baseUrl the base server URL (e.g. http://localhost:8081)
     * @param state the current boss state snapshot
     * @return CompletableFuture containing the prediction response, or null if unreachable
     */
    public CompletableFuture<BossPredictionResponse> predictBossState(String baseUrl, BossState state) {
        if (baseUrl == null || baseUrl.trim().isEmpty() || state == null) {
            return CompletableFuture.completedFuture(null);
        }

        String targetUrl = baseUrl.endsWith("/") ? baseUrl + "boss/predict" : baseUrl + "/boss/predict";
        String jsonPayload = gson.toJson(state);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            String token = resolveAuthToken();
            if (token != null && !token.isEmpty()) {
                requestBuilder.header(AUTH_HEADER, token);
            }

            HttpRequest request = requestBuilder.build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            String body = response.body();
                            if (body != null && !body.isEmpty()) {
                                return gson.fromJson(body, BossPredictionResponse.class);
                            }
                        } else {
                            log.debug("Agent Server returned status code: {}", response.statusCode());
                        }
                        return null;
                    })
                    .exceptionally(ex -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        if (cause instanceof HttpTimeoutException) {
                            log.debug("Agent Server request timed out: {}", cause.getMessage());
                        } else {
                            log.debug("Agent Server communication failure: {}", cause.getMessage());
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.debug("Failed to build request to Agent Server: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    private synchronized String resolveAuthToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && (now - lastTokenReadTime < 10_000)) {
            return cachedToken;
        }

        lastTokenReadTime = now;
        String userHome = System.getProperty("user.home");
        Path[] candidateFiles = new Path[]{
                Paths.get(userHome, ".runelite", ".agent.token"),
                Paths.get(userHome, ".runelite", ".agent-token")
        };

        for (Path file : candidateFiles) {
            if (Files.exists(file)) {
                try {
                    String token = Files.readString(file).trim();
                    if (!token.isEmpty()) {
                        cachedToken = token;
                        return cachedToken;
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return null;
    }
}
