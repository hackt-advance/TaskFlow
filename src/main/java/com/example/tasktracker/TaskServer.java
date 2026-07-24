package com.example.tasktracker;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class TaskServer {
    private final HttpServer server;

    public TaskServer(int port) throws IOException {
        TaskRepository repository = new TaskRepository();
        TaskController controller = new TaskController(repository);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", controller::handleHealth);
        server.createContext("/api/tasks", controller::handleTasks);
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }
}

