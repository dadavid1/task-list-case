# Task List Application

A small task list application for managing projects, tasks, completion status, and deadlines.

The application started as a console-only program. As part of this implementation, the core logic was extracted away from the console layer so that the same business logic can now be used from both:

- a console interface
- a REST API

The application keeps data in memory.

---

## Features

### Console features

The console application supports:

- creating projects
- creating tasks inside projects
- checking and unchecking tasks
- assigning deadlines to tasks
- showing all tasks grouped by project
- showing tasks due today
- showing tasks grouped by deadline

### REST API features

The REST API supports:

- creating projects
- listing projects with their tasks
- creating tasks inside projects
- updating task deadlines
- viewing tasks grouped by deadline

Swagger UI is also available for easier API exploration.

---

## Technology stack

- Java 21
- Spring Boot 3.3.4
- Maven
- JUnit 5
- MockMvc
- springdoc-openapi / Swagger UI

---

## Architecture

The code is organized by application layer:

```text
com.ortecfinance.tasklist
  TaskListApplication

com.ortecfinance.tasklist.cli
  TaskList

com.ortecfinance.tasklist.model
  Project
  Task
  DeadlineGroup

com.ortecfinance.tasklist.service
  TaskListService

com.ortecfinance.tasklist.exception
  ProjectNotFoundException
  TaskNotFoundException

com.ortecfinance.tasklist.web
  ProjectController
  RestExceptionHandler
  OpenApiConfig

com.ortecfinance.tasklist.web.dto
  CreateProjectRequest
  CreateTaskRequest
  ProjectResponse
  TaskResponse
  DeadlineGroupResponse
  ErrorResponse
```

### Design Decisions

The main design goal was to separate the reusable application logic from the user interfaces.

`TaskListService` contains the core business logic. The console interface and the REST controller both delegate to this same service, making it easier to add another interface later (such as a GUI) without duplicating task management logic.

The REST API uses DTOs instead of exposing internal model objects directly, keeping the API contract separate from the internal implementation.

Deadlines are represented with `LocalDate`, using the `dd-MM-yyyy` format for user input and API responses.

The `today` functionality uses an injectable `Clock`, which makes date-dependent behavior deterministic and easier to test.

**Project identifier note:** The original console application identifies projects by name (e.g. `add task <project name> <task description>`). The original model does not define a separate numeric project ID. For that reason, the REST API also uses the project name as the project identifier in paths such as `/projects/{projectName}/tasks`, keeping the REST API aligned with the existing domain model.

---

## Running the Application

### Tests

```bash
mvn test
```

### Console Mode

```bash
mvn spring-boot:run
```

**Example session:**
1. add project training
2. add task training SOLID 
3. deadline 1 25-11-2024 
4. check 1 
5. show 
6. view-by-deadline 
7. today 
8. quit

**Available commands:**

| Command | Description |
|---|---|
| `show` | Show all tasks grouped by project |
| `add project <name>` | Create a new project |
| `add task <project> <description>` | Add a task to a project |
| `check <task ID>` | Mark a task as done |
| `uncheck <task ID>` | Mark a task as not done |
| `deadline <task ID> <date>` | Assign a deadline (`dd-MM-yyyy`) |
| `view-by-deadline` | Show tasks grouped by deadline |
| `today` | Show tasks due today |
| `help` | Show available commands |
| `quit` | Exit the application |

### REST API

**Windows PowerShell:**
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=web"
```

**macOS/Linux/Git Bash:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=web
```

- API base URL: `http://localhost:8080/projects`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`

---

## REST API Reference

### Create a project
POST /projects

Request body:
```json
{ "name": "training" }
```

Response: `201 Created`

---

### Get all projects and tasks
GET /projects

Response:
```json
[
  {
    "name": "training",
    "tasks": [
      {
        "id": 1,
        "description": "SOLID",
        "done": false,
        "deadline": "25-11-2024"
      }
    ]
  }
]
```

---

### Create a task inside a project
POST /projects/{projectName}/tasks

Request body:
```json
{ "description": "SOLID" }
```

Response: `201 Created`

---

### Update a task deadline
PUT /projects/{projectName}/tasks/{taskId}?deadline=25-11-2024

Response: `200 OK`

---

### View tasks grouped by deadline
GET /projects/view_by_deadline

Response:
```json
[
  {
    "deadline": "25-11-2024",
    "projects": [
      {
        "name": "training",
        "tasks": [
          { "id": 1, "description": "SOLID", "done": false, "deadline": "25-11-2024" }
        ]
      }
    ]
  },
  {
    "deadline": null,
    "projects": [
      {
        "name": "training",
        "tasks": [
          { "id": 2, "description": "Refactor the codebase", "done": false, "deadline": null }
        ]
      }
    ]
  }
]
```

Tasks with deadlines are ordered chronologically. Tasks without a deadline are placed at the end.

---

### Error Responses

The API returns structured error responses for all failure cases.

```json
{ "message": "Could not find a project with the name \"missing\"." }
```

```json
{ "message": "Invalid date. Please use format dd-MM-yyyy." }
```

Possible error cases: missing project, missing task, invalid deadline format.

---

## Example API Flow

```bash
# Create a project
curl -X POST http://localhost:8080/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"training"}'

# Create a task
curl -X POST http://localhost:8080/projects/training/tasks \
  -H "Content-Type: application/json" \
  -d '{"description":"SOLID"}'

# Set a deadline
curl -X PUT "http://localhost:8080/projects/training/tasks/1?deadline=25-11-2024"

# Get all projects
curl http://localhost:8080/projects

# Get tasks grouped by deadline
curl http://localhost:8080/projects/view_by_deadline
```

---

## Testing Strategy

Tests are organized at three levels:

**Service tests** (`TaskListServiceTest`) — core business logic including project and task creation, completion status, deadline assignment, tasks due today, grouping by deadline, error cases, and protection against external modification of returned collections.

**Console tests** (`ApplicationTest`) — console behavior including command parsing, user-facing output, invalid command handling, deadline commands, today view, and deadline grouping view.

**REST tests** (`ProjectControllerTest`) — REST API via Spring Boot and MockMvc, covering project creation, project listing, task creation, deadline updates, deadline grouped view, and REST error responses.

Swagger UI was added as a development and review aid for manual browser-based exploration. Correctness is covered by the automated test suite.

---

## Notes and Trade-offs

- Data is stored in memory only; nothing is persisted after the application stops.
- Project names are used as identifiers because the original model does not include project IDs.
- The REST API and console interface reuse the same core service (`TaskListService`).
- The deadline format `dd-MM-yyyy` is consistent across both the console and REST interfaces.
- Commits are atomic and focused to make feature work, refactoring, testing, and documentation easy to review separately.