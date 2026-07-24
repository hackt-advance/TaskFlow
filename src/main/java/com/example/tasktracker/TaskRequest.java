package com.example.tasktracker;

public record TaskRequest(
        String title,
        String description,
        String priority,
        String dueDate
) {
}

