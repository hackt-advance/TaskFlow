package com.example.tasktracker;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

public final class TaskValidator {
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private TaskValidator() {
    }

    public static void validate(TaskRequest request) throws ValidationException {
        if (request.title() == null || request.title().trim().length() < 3 || request.title().trim().length() > 100) {
            throw new ValidationException("title must be between 3 and 100 characters");
        }

        if (request.description() != null && request.description().trim().length() > 500) {
            throw new ValidationException("description must be 500 characters or fewer");
        }

        if (request.priority() == null || !ALLOWED_PRIORITIES.contains(request.priority().trim().toUpperCase())) {
            throw new ValidationException("priority must be one of LOW, MEDIUM, or HIGH");
        }

        if (request.dueDate() != null && !request.dueDate().isBlank()) {
            try {
                LocalDate.parse(request.dueDate().trim());
            } catch (DateTimeParseException exception) {
                throw new ValidationException("dueDate must use YYYY-MM-DD format");
            }
        }
    }
}

