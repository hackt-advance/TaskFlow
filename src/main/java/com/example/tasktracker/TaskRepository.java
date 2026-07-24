package com.example.tasktracker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TaskRepository {
    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    public List<Task> findAll() {
        List<Task> allTasks = new ArrayList<>(tasks.values());
        allTasks.sort(Comparator.comparing(Task::createdAt));
        return allTasks;
    }

    public Optional<Task> findById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Task create(TaskRequest request) {
        Instant now = Instant.now();
        Task task = new Task(
                UUID.randomUUID().toString(),
                request.title().trim(),
                sanitizeDescription(request.description()),
                TaskPriority.valueOf(request.priority().trim().toUpperCase()),
                request.dueDate(),
                now,
                now
        );
        tasks.put(task.id(), task);
        return task;
    }

    public Optional<Task> update(String id, TaskRequest request) {
        Task existing = tasks.get(id);
        if (existing == null) {
            return Optional.empty();
        }

        Task updated = new Task(
                existing.id(),
                request.title().trim(),
                sanitizeDescription(request.description()),
                TaskPriority.valueOf(request.priority().trim().toUpperCase()),
                request.dueDate(),
                existing.createdAt(),
                Instant.now()
        );
        tasks.put(id, updated);
        return Optional.of(updated);
    }

    public boolean delete(String id) {
        return tasks.remove(id) != null;
    }

    private String sanitizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.trim();
    }
}

