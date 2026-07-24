package com.example.tasktracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class JsonUtils {
    private JsonUtils() {
    }

    public static Map<String, String> parseFlatJson(String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON body is required");
        }

        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON object");
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        Map<String, String> values = new HashMap<>();
        if (content.isEmpty()) {
            return values;
        }

        for (String pair : splitTopLevel(content)) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid key/value pair");
            }

            String key = stripQuotes(keyValue[0].trim());
            String rawValue = keyValue[1].trim();
            if ("null".equals(rawValue)) {
                values.put(key, null);
            } else {
                values.put(key, stripQuotes(rawValue));
            }
        }

        return values;
    }

    public static String toTaskJson(Task task) {
        return "{"
                + "\"id\":\"" + escape(task.id()) + "\","
                + "\"title\":\"" + escape(task.title()) + "\","
                + "\"description\":\"" + escape(task.description()) + "\","
                + "\"priority\":\"" + task.priority().name() + "\","
                + "\"dueDate\":" + nullableString(task.dueDate()) + ","
                + "\"createdAt\":\"" + task.createdAt() + "\","
                + "\"updatedAt\":\"" + task.updatedAt() + "\""
                + "}";
    }

    public static String toTaskListJson(List<Task> tasks) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Task task : tasks) {
            joiner.add(toTaskJson(task));
        }
        return joiner.toString();
    }

    public static String toErrorJson(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    private static List<String> splitTopLevel(String content) {
        java.util.ArrayList<String> pairs = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }

            if (ch == ',' && !inQuotes) {
                pairs.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }

        return pairs;
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return unescape(trimmed.substring(1, trimmed.length() - 1));
        }
        throw new IllegalArgumentException("Expected quoted JSON string");
    }

    private static String nullableString(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unescape(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }
}

