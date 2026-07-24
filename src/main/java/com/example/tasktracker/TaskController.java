package com.example.tasktracker;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

public class TaskController {
    private final TaskRepository repository;

    public TaskController(TaskRepository repository) {
        this.repository = repository;
    }

    public void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD,
                    "{\"error\":\"Method not allowed\"}");
            return;
        }

        ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_OK,
                "{\"status\":\"ok\",\"service\":\"task-tracker-api\"}");
    }

    public void handleTasks(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/api/tasks".equals(path)) {
            handleCollection(exchange, method);
            return;
        }

        if (path.startsWith("/api/tasks/")) {
            String id = path.substring("/api/tasks/".length()).trim();
            handleItem(exchange, method, id);
            return;
        }

        ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND,
                "{\"error\":\"Route not found\"}");
    }

    private void handleCollection(HttpExchange exchange, String method) throws IOException {
        if ("GET".equalsIgnoreCase(method)) {
            ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_OK,
                    JsonUtils.toTaskListJson(repository.findAll()));
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            try {
                TaskRequest request = parseAndValidate(exchange);
                Task created = repository.create(request);
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_CREATED,
                        JsonUtils.toTaskJson(created));
            } catch (ValidationException exception) {
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                        JsonUtils.toErrorJson(exception.getMessage()));
            } catch (IllegalArgumentException exception) {
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                        JsonUtils.toErrorJson("Request body must be valid JSON"));
            }
            return;
        }

        ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD,
                "{\"error\":\"Method not allowed\"}");
    }

    private void handleItem(HttpExchange exchange, String method, String id) throws IOException {
        if (id.isBlank()) {
            ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                    JsonUtils.toErrorJson("Task id is required"));
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            Optional<Task> task = repository.findById(id);
            if (task.isEmpty()) {
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND,
                        JsonUtils.toErrorJson("Task not found"));
                return;
            }
            ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_OK,
                    JsonUtils.toTaskJson(task.get()));
            return;
        }

        if ("PUT".equalsIgnoreCase(method)) {
            try {
                TaskRequest request = parseAndValidate(exchange);
                Optional<Task> updated = repository.update(id, request);
                if (updated.isEmpty()) {
                    ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND,
                            JsonUtils.toErrorJson("Task not found"));
                    return;
                }
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_OK,
                        JsonUtils.toTaskJson(updated.get()));
            } catch (ValidationException exception) {
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                        JsonUtils.toErrorJson(exception.getMessage()));
            } catch (IllegalArgumentException exception) {
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                        JsonUtils.toErrorJson("Request body must be valid JSON"));
            }
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            boolean deleted = repository.delete(id);
            if (!deleted) {
                ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND,
                        JsonUtils.toErrorJson("Task not found"));
                return;
            }
            ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_OK,
                    "{\"message\":\"Task deleted\"}");
            return;
        }

        ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD,
                "{\"error\":\"Method not allowed\"}");
    }

    private TaskRequest parseAndValidate(HttpExchange exchange) throws IOException, ValidationException {
        String body = ResponseUtils.readBody(exchange);
        Map<String, String> values = JsonUtils.parseFlatJson(body);
        TaskRequest request = new TaskRequest(
                values.get("title"),
                values.get("description"),
                values.get("priority"),
                values.get("dueDate")
        );
        TaskValidator.validate(request);
        return request;
    }
}

