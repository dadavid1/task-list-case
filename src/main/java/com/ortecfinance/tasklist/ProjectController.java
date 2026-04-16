package com.ortecfinance.tasklist;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import com.ortecfinance.tasklist.dto.*;

import java.util.List;

/**
 * REST Controller that exposes the TaskList application over HTTP.
 * Maps incoming web requests to the core business logic in the TaskListService.
 */
@RestController
@RequestMapping("/projects")
public final class ProjectController {
    private final TaskListService service;

    /**
     * The standard date formatter used to parse deadlines via the API.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Constructs the controller with the necessary business service.
     * Spring automatically injects the TaskListService here
     *
     * @param service the core business logic engine
     */
    public ProjectController(TaskListService service) {
        this.service = service;
    }

    /**
     * Creates a new project via a POST request.
     *
     * @param request a payload containing the new project's name
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createProject(@RequestBody CreateProjectRequest request) {
        service.addProject(request.name());
    }

    /**
     * Retrieves a complete list of all projects and their associated tasks.
     * Converts internal domain objects (Project, Task) into safe API response objects (DTOs).
     *
     * @return a list of serialized project data
     */
    @GetMapping
    public List<ProjectResponse> getProjects() {
        return service.getProjects().stream()
                .map(project -> new ProjectResponse(
                        project.getName(),
                        project.getTasks().stream()
                                .map(task -> new TaskResponse(
                                        task.getId(),
                                        task.getDescription(),
                                        task.isDone(),
                                        task.getDeadline() == null ? null : task.getDeadline().format(DATE_FORMATTER)
                                ))
                                .toList()
                ))
                .toList();
    }
}
