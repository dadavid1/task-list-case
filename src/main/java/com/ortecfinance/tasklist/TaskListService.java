package com.ortecfinance.tasklist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.LocalDate;

/**
 * The core service that manages all projects and tasks.
 */
public final class TaskListService {
    private final Map<String, Project> projects = new LinkedHashMap<>();
    private final Clock clock;
    private long lastId = 0;

    /**
     * Constructs a new TaskListService using the system's default time zone.
     */
    public TaskListService() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Constructs a new TaskListService with a specific clock, useful for testing.
     *
     * @param clock the clock to use for date/time operations
     */
    public TaskListService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Gets a lsit of all existing projects.
     *
     * @return a read-only list of projects
     */
    public List<Project> getProjects() {
        return List.copyOf(projects.values());
    }

    /**
     * Creates a new proejct and adds it to the list.
     *
     * @param name the name of the new project
     */
    public void addProject(String name) {
        projects.put(name, new Project(name));
    }

    /**
     * Adds a new task to a specific project.
     *
     * @param projectName the name of the project to add the task to
     * @param description what the task is about
     * @return the newly created task
     * @throws ProjectNotFoundException if the project does not exist
     */
    public Task addTask(String projectName, String description) {
        Project project = projects.get(projectName);
        if (project == null) {
            throw new ProjectNotFoundException(projectName);
        }

        return project.addTask(nextId(), description);
    }

    /**
     * Marks a specific task as completed.
     *
     * @param taskId the ID of the task to check
     */
    public void checkTask(long taskId) {
        setDone(taskId, true);
    }

    /**
     * Marks a specific task as not completed.
     *
     * @param taskId the ID of the task to uncheck
     */
    public void uncheckTask(long taskId) {
        setDone(taskId, false);
    }

    /**
     * Sets a deadline for a specific task.
     *
     * @param taskId the ID of the task
     * @param deadline the date by which the task should be completed
     * @throws TaskNotFoundException if the task ID does not exist
     */
    public void setDeadline(long taskId, LocalDate deadline) {
        Task task = findTaskById(taskId);
        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }

        task.setDeadline(deadline);
    }

    /**
     * Retrieves a list of projects containing only the tasks that are due today.
     * Empty projects are filtered out.
     *
     * @return a list of projects with tasks due today
     */
    public List<Project> getProjectsWithTasksDueToday() {
        LocalDate today = LocalDate.now(clock);

        return projects.values().stream()
                .map(project -> copyProjectWithMatchingTasks(project,
                        task -> today.equals(task.getDeadline())))
                .filter(project -> !project.getTasks().isEmpty())
                .toList();
    }

    /**
     * Helper method to update the completion status of a task.
     *
     * @param taskId the ID of the task
     * @param done true to check, false to uncheck
     * @throws TaskNotFoundException if the task ID does not exist
     */
    private void setDone(long taskId, boolean done) {
        Task task = findTaskById(taskId);
        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }

        task.setDone(done);
    }

    /**
     * Searches across all projects to find a task by its ID.
     *
     * @param taskId the ID to search for
     * @return the task if found, or null if it does not exist
     */
    private Task findTaskById(long taskId) {
        for (Project project : projects.values()) {
            for (Task task : project.getTasks()) {
                if (task.getId() == taskId) {
                    return task;
                }
            }
        }
        return null;
    }

    /**
     * Creates a copy of a project containing only tasks that match a specific condition.
     *
     * @param source the original project to copy from
     * @param predicate the condition that tasks must meet to be included
     * @return a new project instance containing the filtered tasks
     */
    private Project copyProjectWithMatchingTasks(Project source, java.util.function.Predicate<Task> predicate) {
        Project project = new Project(source.getName());

        for (Task task : source.getTasks()) {
            if (predicate.test(task)) {
                Task copiedTask = project.addTask(task.getId(), task.getDescription());
                copiedTask.setDone(task.isDone());
                copiedTask.setDeadline(task.getDeadline());
            }
        }

        return project;
    }

    /**
     * Generates the next sequential ID for a new task.
     *
     * @return the new ID
     */
    private long nextId() {
        return ++lastId;
    }
}
