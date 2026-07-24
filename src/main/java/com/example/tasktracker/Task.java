package com.example.tasktracker;

import java.time.Instant;

public record Task(
        String id,
        String title,
        String description,
        TaskPriority priority,
        String dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}

