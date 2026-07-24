package com.example.tasktracker;

public class TaskTrackerApplication {
    public static void main(String[] args) throws Exception {
        int port = resolvePort();
        TaskServer server = new TaskServer(port);
        server.start();
        System.out.println("Task Tracker API running on port " + port);
    }

    private static int resolvePort() {
        String portValue = System.getenv("PORT");
        if (portValue == null || portValue.isBlank()) {
            return 8080;
        }
        return Integer.parseInt(portValue);
    }
}
