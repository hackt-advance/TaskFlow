package com.example.tasktracker;

public class TaskTrackerApplication {
    public static void main(String[] args) throws Exception {
        TaskServer server = new TaskServer(8080);
        server.start();
        System.out.println("Task Tracker API running on http://localhost:8080");
    }
}

