package com.ortecfinance.tasklist.web;

import com.ortecfinance.tasklist.service.TaskListService;
import com.ortecfinance.tasklist.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller that exposes the TaskList application over HTTP.
 * Maps incoming web requests to the core business logic in the TaskListService.
 */
@RestController
@RequestMapping("/projects")
@Tag(name = "Projects", description = "Operations for managing projects, tasks, and deadlines")
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
    @Operation(summary = "Create a new project")
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
    @Operation(summary = "Get all projects with their tasks")
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
    /**
     * Creates a new task inside a specific existing project.
     *
     * @param projectName the name of the project to add the task to
     * @param request a payload containing the task description
     */
    @PostMapping("/{projectName}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task inside a project")
    public void createTask(@PathVariable String projectName, @RequestBody CreateTaskRequest request) {
        service.addTask(projectName, request.description());
    }

    /**
     * Updates the deadline of a task inside a specific project.
     *
     * @param projectName the project that should contain the task
     * @param taskId the ID of the task to update
     * @param deadline the new deadline in dd-MM-yyyy format
     */
    @PutMapping("/{projectName}/tasks/{taskId}")
    @Operation(summary = "Update the deadline of a task")
    public void updateTaskDeadline(@PathVariable String projectName,
                                   @PathVariable long taskId,
                                   @RequestParam String deadline) {
        LocalDate parsedDeadline = LocalDate.parse(deadline, DATE_FORMATTER);
        service.setDeadline(projectName, taskId, parsedDeadline);
    }

    /**
     * Returns all tasks grouped by deadline and then by project.
     * Tasks are grouped chronologically, while tasks without a deadline
     * are placed in a final "No deadline" group represented by a null deadline.
     *
     * @return tasks grouped by deadline and then by project
     */
    @GetMapping("/view_by_deadline")
    @Operation(summary = "Get tasks grouped by deadline")
    public List<DeadlineGroupResponse> viewByDeadline() {
        return service.getTasksGroupedByDeadline().stream()
                .map(group -> new DeadlineGroupResponse(
                        group.getDeadline() == null
                                ? null
                                : group.getDeadline().format(DATE_FORMATTER),
                        group.getProjects().stream()
                                .map(project -> new ProjectResponse(
                                        project.getName(),
                                        project.getTasks().stream()
                                                .map(task -> new TaskResponse(
                                                        task.getId(),
                                                        task.getDescription(),
                                                        task.isDone(),
                                                        task.getDeadline() == null
                                                                ? null
                                                                : task.getDeadline().format(DATE_FORMATTER)
                                                ))
                                                .toList()
                                ))
                                .toList()
                ))
                .toList();
    }
}
