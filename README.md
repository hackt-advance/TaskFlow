# Task Tracker API

A small Java REST API that demonstrates:

- HTTP endpoints
- request validation
- automated integration tests
- Docker deployment
- GitHub Actions CI

The project uses only the Java standard library so it can run with a plain JDK.

## Features

- `GET /health` returns service status
- `GET /api/tasks` lists all tasks
- `GET /api/tasks/{id}` fetches a task by id
- `POST /api/tasks` creates a task
- `PUT /api/tasks/{id}` updates a task
- `DELETE /api/tasks/{id}` removes a task

## Task payload

```json
{
  "title": "Prepare GitHub project",
  "description": "Add README, tests, and deployment files",
  "priority": "HIGH",
  "dueDate": "2026-08-10"
}
```

Validation rules:

- `title` is required and must be 3-100 characters
- `description` is optional and must be at most 500 characters
- `priority` is required and must be `LOW`, `MEDIUM`, or `HIGH`
- `dueDate` is optional and must use `YYYY-MM-DD`

## Run locally

Compile the project:

```bash
javac -d out $(find src/main/java src/test/java -name "*.java")
```

Run the API:

```bash
java -cp out com.example.tasktracker.TaskTrackerApplication
```

The server starts on `http://localhost:8080`.

## Run tests

```bash
java -cp out com.example.tasktracker.TaskApiTest
```

## Example requests

Create a task:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Deploy service","description":"Prepare Docker image","priority":"HIGH","dueDate":"2026-08-15"}'
```

List tasks:

```bash
curl http://localhost:8080/api/tasks
```

## Docker

Build the image:

```bash
docker build -t task-tracker-api .
```

Run the container:

```bash
docker run -p 8080:8080 task-tracker-api
```

## GitHub Actions

The workflow at `.github/workflows/ci.yml` compiles the project and runs the integration tests on every push and pull request.

## Suggested GitHub repo steps

1. Create a new GitHub repository.
2. Copy this project or commit it from this workspace.
3. Push with:

```bash
git init
git add .
git commit -m "Add Java task tracker API project"
git branch -M main
git remote add origin <your-github-repo-url>
git push -u origin main
```

