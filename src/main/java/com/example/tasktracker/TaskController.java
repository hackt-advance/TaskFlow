package com.example.tasktracker;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

public class TaskController {
    private final TaskRepository repository;
    private final String homepageHtml;

    public TaskController(TaskRepository repository) {
        this.repository = repository;
        this.homepageHtml = buildHomepage();
    }

    public void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtils.sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD,
                    "{\"error\":\"Method not allowed\"}");
            return;
        }

        ResponseUtils.sendHtml(exchange, HttpURLConnection.HTTP_OK, homepageHtml);
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

    private String buildHomepage() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>TaskFlow</title>
                  <style>
                    :root {
                      --bg: #f5efe4;
                      --paper: rgba(255, 251, 245, 0.84);
                      --panel: rgba(255, 255, 255, 0.76);
                      --ink: #1f2a2a;
                      --muted: #5f6c68;
                      --accent: #e76f51;
                      --accent-strong: #cc5333;
                      --teal: #287271;
                      --sand: #e9c46a;
                      --line: rgba(31, 42, 42, 0.12);
                      --shadow: 0 24px 60px rgba(69, 52, 37, 0.12);
                    }

                    * { box-sizing: border-box; }

                    body {
                      margin: 0;
                      font-family: Georgia, "Times New Roman", serif;
                      color: var(--ink);
                      background:
                        radial-gradient(circle at top left, rgba(233, 196, 106, 0.45), transparent 30%),
                        radial-gradient(circle at top right, rgba(40, 114, 113, 0.18), transparent 28%),
                        linear-gradient(160deg, #f8f4ea 0%, #f3ebda 46%, #efe5d1 100%);
                      min-height: 100vh;
                    }

                    .shell {
                      width: min(1180px, calc(100% - 32px));
                      margin: 0 auto;
                      padding: 28px 0 42px;
                    }

                    .hero {
                      position: relative;
                      overflow: hidden;
                      border: 1px solid rgba(255, 255, 255, 0.5);
                      border-radius: 28px;
                      padding: 34px;
                      background:
                        linear-gradient(145deg, rgba(255,255,255,0.76), rgba(255,255,255,0.5)),
                        linear-gradient(120deg, rgba(231,111,81,0.14), rgba(40,114,113,0.06));
                      box-shadow: var(--shadow);
                      backdrop-filter: blur(10px);
                    }

                    .hero::after {
                      content: "";
                      position: absolute;
                      inset: auto -50px -70px auto;
                      width: 220px;
                      height: 220px;
                      border-radius: 50%;
                      background: radial-gradient(circle, rgba(231,111,81,0.28), transparent 65%);
                    }

                    .eyebrow {
                      display: inline-flex;
                      align-items: center;
                      gap: 10px;
                      padding: 8px 14px;
                      border-radius: 999px;
                      background: rgba(40,114,113,0.1);
                      color: var(--teal);
                      font-size: 13px;
                      letter-spacing: 0.12em;
                      text-transform: uppercase;
                    }

                    .hero-grid {
                      display: grid;
                      grid-template-columns: 1.3fr 0.8fr;
                      gap: 28px;
                      align-items: end;
                      margin-top: 22px;
                    }

                    h1 {
                      font-size: clamp(2.5rem, 5vw, 5rem);
                      line-height: 0.95;
                      margin: 0 0 16px;
                      letter-spacing: -0.04em;
                    }

                    .lead {
                      max-width: 640px;
                      font-family: "Helvetica Neue", Arial, sans-serif;
                      font-size: 1.05rem;
                      line-height: 1.7;
                      color: var(--muted);
                      margin: 0;
                    }

                    .stats {
                      display: grid;
                      grid-template-columns: repeat(2, minmax(0, 1fr));
                      gap: 14px;
                    }

                    .stat {
                      border-radius: 22px;
                      padding: 20px;
                      background: rgba(255,255,255,0.6);
                      border: 1px solid rgba(255,255,255,0.5);
                    }

                    .stat strong {
                      display: block;
                      font-size: 2rem;
                      margin-bottom: 6px;
                    }

                    .stat span {
                      font-family: "Helvetica Neue", Arial, sans-serif;
                      color: var(--muted);
                    }

                    .dashboard {
                      display: grid;
                      grid-template-columns: 0.95fr 1.05fr;
                      gap: 24px;
                      margin-top: 24px;
                    }

                    .card {
                      background: var(--paper);
                      border: 1px solid rgba(255,255,255,0.6);
                      border-radius: 26px;
                      padding: 24px;
                      box-shadow: var(--shadow);
                    }

                    .card h2 {
                      margin: 0 0 10px;
                      font-size: 1.6rem;
                    }

                    .card p,
                    .endpoint,
                    .task-meta,
                    .empty,
                    .status-note {
                      font-family: "Helvetica Neue", Arial, sans-serif;
                      color: var(--muted);
                    }

                    .form-grid {
                      display: grid;
                      gap: 14px;
                    }

                    label {
                      display: grid;
                      gap: 8px;
                      font-family: "Helvetica Neue", Arial, sans-serif;
                      font-size: 0.94rem;
                    }

                    input,
                    textarea,
                    select,
                    button {
                      font: inherit;
                    }

                    input,
                    textarea,
                    select {
                      width: 100%;
                      border: 1px solid var(--line);
                      border-radius: 16px;
                      padding: 13px 14px;
                      background: rgba(255,255,255,0.92);
                      color: var(--ink);
                    }

                    textarea {
                      min-height: 120px;
                      resize: vertical;
                    }

                    .row {
                      display: grid;
                      grid-template-columns: repeat(2, minmax(0, 1fr));
                      gap: 14px;
                    }

                    .actions {
                      display: flex;
                      gap: 12px;
                      align-items: center;
                      margin-top: 8px;
                      flex-wrap: wrap;
                    }

                    .primary {
                      background: linear-gradient(135deg, var(--accent), var(--accent-strong));
                      color: white;
                      border: 0;
                      padding: 13px 18px;
                      border-radius: 999px;
                      cursor: pointer;
                      box-shadow: 0 16px 32px rgba(231,111,81,0.26);
                    }

                    .secondary {
                      background: transparent;
                      color: var(--teal);
                      border: 1px solid rgba(40,114,113,0.25);
                      padding: 13px 18px;
                      border-radius: 999px;
                      cursor: pointer;
                    }

                    .status {
                      min-height: 24px;
                      font-family: "Helvetica Neue", Arial, sans-serif;
                      font-size: 0.94rem;
                    }

                    .status.success { color: var(--teal); }
                    .status.error { color: var(--accent-strong); }

                    .stack {
                      display: grid;
                      gap: 16px;
                    }

                    .task-list {
                      display: grid;
                      gap: 14px;
                    }

                    .task {
                      background: var(--panel);
                      border: 1px solid rgba(255,255,255,0.7);
                      border-radius: 22px;
                      padding: 18px;
                    }

                    .task-head {
                      display: flex;
                      justify-content: space-between;
                      align-items: flex-start;
                      gap: 12px;
                    }

                    .task h3 {
                      margin: 0 0 8px;
                      font-size: 1.2rem;
                    }

                    .badge {
                      border-radius: 999px;
                      padding: 7px 12px;
                      font-family: "Helvetica Neue", Arial, sans-serif;
                      font-size: 0.76rem;
                      text-transform: uppercase;
                      letter-spacing: 0.08em;
                      white-space: nowrap;
                    }

                    .badge.HIGH { background: rgba(231,111,81,0.14); color: var(--accent-strong); }
                    .badge.MEDIUM { background: rgba(233,196,106,0.24); color: #8d6b0c; }
                    .badge.LOW { background: rgba(40,114,113,0.12); color: var(--teal); }

                    .task p {
                      margin: 0 0 12px;
                      line-height: 1.6;
                    }

                    .task-footer {
                      display: flex;
                      justify-content: space-between;
                      gap: 14px;
                      align-items: center;
                      flex-wrap: wrap;
                    }

                    .delete-btn {
                      border: 0;
                      background: rgba(31,42,42,0.08);
                      color: var(--ink);
                      border-radius: 999px;
                      padding: 10px 14px;
                      cursor: pointer;
                    }

                    .endpoints {
                      display: grid;
                      gap: 10px;
                    }

                    .endpoint {
                      display: flex;
                      justify-content: space-between;
                      gap: 16px;
                      padding: 14px 16px;
                      border-radius: 18px;
                      background: rgba(255,255,255,0.62);
                    }

                    code {
                      font-family: "SFMono-Regular", Consolas, monospace;
                      font-size: 0.92rem;
                      color: var(--ink);
                    }

                    .pulse {
                      display: inline-block;
                      width: 10px;
                      height: 10px;
                      border-radius: 50%;
                      background: var(--teal);
                      box-shadow: 0 0 0 0 rgba(40,114,113,0.45);
                      animation: pulse 2s infinite;
                    }

                    @keyframes pulse {
                      0% { box-shadow: 0 0 0 0 rgba(40,114,113,0.45); }
                      70% { box-shadow: 0 0 0 10px rgba(40,114,113,0); }
                      100% { box-shadow: 0 0 0 0 rgba(40,114,113,0); }
                    }

                    @media (max-width: 960px) {
                      .hero-grid,
                      .dashboard {
                        grid-template-columns: 1fr;
                      }
                    }

                    @media (max-width: 680px) {
                      .shell { width: min(100% - 20px, 1180px); padding-top: 20px; }
                      .hero,
                      .card { padding: 20px; border-radius: 22px; }
                      .row,
                      .stats { grid-template-columns: 1fr; }
                      h1 { font-size: 2.6rem; }
                    }
                  </style>
                </head>
                <body>
                  <div class="shell">
                    <section class="hero">
                      <div class="eyebrow"><span class="pulse"></span> Java REST API Demo</div>
                      <div class="hero-grid">
                        <div>
                          <h1>TaskFlow feels alive now.</h1>
                          <p class="lead">
                            A public-facing demo for your Java backend with a warm landing page, live task creation,
                            instant status checks, and the actual REST API running behind it.
                          </p>
                        </div>
                        <div class="stats">
                          <div class="stat">
                            <strong id="taskCount">0</strong>
                            <span>tasks currently stored</span>
                          </div>
                          <div class="stat">
                            <strong id="healthLabel">...</strong>
                            <span>service heartbeat</span>
                          </div>
                        </div>
                      </div>
                    </section>

                    <section class="dashboard">
                      <div class="card">
                        <h2>Create a task</h2>
                        <p>Use the real API through this interface. Every task you add here is saved by the backend service.</p>
                        <form id="taskForm" class="form-grid">
                          <label>
                            Title
                            <input id="title" name="title" placeholder="Prepare portfolio demo" required minlength="3" maxlength="100">
                          </label>
                          <label>
                            Description
                            <textarea id="description" name="description" placeholder="Add deployment notes, screenshots, and API examples"></textarea>
                          </label>
                          <div class="row">
                            <label>
                              Priority
                              <select id="priority" name="priority">
                                <option value="HIGH">High</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="LOW">Low</option>
                              </select>
                            </label>
                            <label>
                              Due date
                              <input id="dueDate" name="dueDate" type="date">
                            </label>
                          </div>
                          <div class="actions">
                            <button class="primary" type="submit">Create Task</button>
                            <button class="secondary" type="button" id="refreshBtn">Refresh List</button>
                          </div>
                          <div id="status" class="status" aria-live="polite"></div>
                        </form>
                      </div>

                      <div class="stack">
                        <div class="card">
                          <h2>Live tasks</h2>
                          <p class="status-note">This panel reads directly from <code>/api/tasks</code>.</p>
                          <div id="taskList" class="task-list">
                            <div class="empty">Loading tasks...</div>
                          </div>
                        </div>

                        <div class="card">
                          <h2>Useful endpoints</h2>
                          <div class="endpoints">
                            <div class="endpoint"><code>GET /health</code><span>check service status</span></div>
                            <div class="endpoint"><code>GET /api/tasks</code><span>list all tasks</span></div>
                            <div class="endpoint"><code>POST /api/tasks</code><span>create a task</span></div>
                          </div>
                        </div>
                      </div>
                    </section>
                  </div>

                  <script>
                    const taskList = document.getElementById("taskList");
                    const taskCount = document.getElementById("taskCount");
                    const healthLabel = document.getElementById("healthLabel");
                    const statusNode = document.getElementById("status");
                    const form = document.getElementById("taskForm");
                    const refreshBtn = document.getElementById("refreshBtn");

                    async function fetchHealth() {
                      try {
                        const response = await fetch("/health");
                        if (!response.ok) {
                          throw new Error("Health check failed");
                        }
                        const data = await response.json();
                        healthLabel.textContent = data.status === "ok" ? "Healthy" : "Unknown";
                      } catch (error) {
                        healthLabel.textContent = "Unavailable";
                      }
                    }

                    function setStatus(message, type) {
                      statusNode.textContent = message;
                      statusNode.className = "status " + (type || "");
                    }

                    function escapeHtml(value) {
                      return value
                        .replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;")
                        .replaceAll('"', "&quot;");
                    }

                    function renderTasks(tasks) {
                      taskCount.textContent = String(tasks.length);

                      if (!tasks.length) {
                        taskList.innerHTML = '<div class="empty">No tasks yet. Create the first one to bring the demo to life.</div>';
                        return;
                      }

                      taskList.innerHTML = tasks.map(task => {
                        const description = task.description && task.description.trim()
                          ? escapeHtml(task.description)
                          : "No description provided.";
                        const dueDate = task.dueDate ? escapeHtml(task.dueDate) : "No due date";
                        return `
                          <article class="task">
                            <div class="task-head">
                              <div>
                                <h3>${escapeHtml(task.title)}</h3>
                                <p>${description}</p>
                              </div>
                              <span class="badge ${task.priority}">${escapeHtml(task.priority)}</span>
                            </div>
                            <div class="task-footer">
                              <div class="task-meta">Due: ${dueDate}</div>
                              <button class="delete-btn" data-id="${escapeHtml(task.id)}">Delete</button>
                            </div>
                          </article>
                        `;
                      }).join("");
                    }

                    async function loadTasks() {
                      try {
                        const response = await fetch("/api/tasks");
                        if (!response.ok) {
                          throw new Error("Could not load tasks");
                        }
                        const tasks = await response.json();
                        renderTasks(tasks);
                      } catch (error) {
                        taskList.innerHTML = '<div class="empty">The API is reachable, but tasks could not be loaded right now.</div>';
                      }
                    }

                    async function createTask(event) {
                      event.preventDefault();
                      const payload = {
                        title: form.title.value.trim(),
                        description: form.description.value.trim(),
                        priority: form.priority.value,
                        dueDate: form.dueDate.value || null
                      };

                      try {
                        const response = await fetch("/api/tasks", {
                          method: "POST",
                          headers: { "Content-Type": "application/json" },
                          body: JSON.stringify(payload)
                        });

                        const data = await response.json();
                        if (!response.ok) {
                          throw new Error(data.error || "Task creation failed");
                        }

                        form.reset();
                        form.priority.value = "HIGH";
                        setStatus("Task created successfully.", "success");
                        await loadTasks();
                      } catch (error) {
                        setStatus(error.message, "error");
                      }
                    }

                    async function deleteTask(id) {
                      try {
                        const response = await fetch("/api/tasks/" + encodeURIComponent(id), {
                          method: "DELETE"
                        });
                        const data = await response.json();
                        if (!response.ok) {
                          throw new Error(data.error || "Delete failed");
                        }
                        setStatus("Task deleted.", "success");
                        await loadTasks();
                      } catch (error) {
                        setStatus(error.message, "error");
                      }
                    }

                    taskList.addEventListener("click", (event) => {
                      const button = event.target.closest(".delete-btn");
                      if (!button) {
                        return;
                      }
                      deleteTask(button.dataset.id);
                    });

                    form.addEventListener("submit", createTask);
                    refreshBtn.addEventListener("click", loadTasks);

                    fetchHealth();
                    loadTasks();
                  </script>
                </body>
                </html>
                """;
    }
}
