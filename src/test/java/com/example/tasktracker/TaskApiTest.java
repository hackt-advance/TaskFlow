package com.example.tasktracker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TaskApiTest {
    public static void main(String[] args) throws Exception {
        TaskServer server = new TaskServer(8091);
        server.start();

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> health = send(client, "GET", "/health", null);
            assertStatus(health, 200);
            assertContains(health.body(), "\"status\":\"ok\"");

            String createBody = """
                    {"title":"Write GitHub README","description":"Document API usage","priority":"HIGH","dueDate":"2026-08-10"}
                    """;
            HttpResponse<String> created = send(client, "POST", "/api/tasks", createBody);
            assertStatus(created, 201);
            assertContains(created.body(), "\"title\":\"Write GitHub README\"");
            String taskId = extractValue(created.body(), "id");

            HttpResponse<String> list = send(client, "GET", "/api/tasks", null);
            assertStatus(list, 200);
            assertContains(list.body(), taskId);

            HttpResponse<String> single = send(client, "GET", "/api/tasks/" + taskId, null);
            assertStatus(single, 200);
            assertContains(single.body(), "\"priority\":\"HIGH\"");

            String updateBody = """
                    {"title":"Update GitHub README","description":"Add deployment instructions","priority":"MEDIUM","dueDate":"2026-08-11"}
                    """;
            HttpResponse<String> updated = send(client, "PUT", "/api/tasks/" + taskId, updateBody);
            assertStatus(updated, 200);
            assertContains(updated.body(), "\"title\":\"Update GitHub README\"");

            String invalidBody = """
                    {"title":"No","description":"short","priority":"URGENT","dueDate":"2026/08/10"}
                    """;
            HttpResponse<String> invalid = send(client, "POST", "/api/tasks", invalidBody);
            assertStatus(invalid, 400);
            assertContains(invalid.body(), "\"error\"");

            HttpResponse<String> deleted = send(client, "DELETE", "/api/tasks/" + taskId, null);
            assertStatus(deleted, 200);

            HttpResponse<String> missing = send(client, "GET", "/api/tasks/" + taskId, null);
            assertStatus(missing, 404);

            System.out.println("All integration tests passed.");
        } finally {
            server.stop(0);
        }
    }

    private static HttpResponse<String> send(HttpClient client, String method, String path, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8091" + path))
                .header("Content-Type", "application/json");

        switch (method) {
            case "GET" -> builder.GET();
            case "DELETE" -> builder.DELETE();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body));
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertStatus(HttpResponse<String> response, int expected) {
        if (response.statusCode() != expected) {
            throw new AssertionError("Expected status " + expected + " but got " + response.statusCode()
                    + " with body: " + response.body());
        }
    }

    private static void assertContains(String body, String expected) {
        if (!body.contains(expected)) {
            throw new AssertionError("Expected body to contain " + expected + " but was: " + body);
        }
    }

    private static String extractValue(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Could not find key " + key + " in response: " + json);
        }
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf("\"", valueStart);
        return json.substring(valueStart, valueEnd);
    }
}

