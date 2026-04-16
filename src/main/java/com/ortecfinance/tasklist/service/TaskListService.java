package com.ortecfinance.tasklist.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.LocalDate;

import com.ortecfinance.tasklist.model.DeadlineGroup;
import com.ortecfinance.tasklist.model.Project;
import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import org.springframework.stereotype.Service;

/**
 * The core service that manages all projects and tasks.
 */
@Service
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
     * Sets a deadline for a task that must belong to the given project.
     * It is needed for REST calls where the project is part of the URL.
     *
     * @param projectName the project that should contain the task
     * @param taskId the task to update
     * @param deadline the new deadline
     */
    public void setDeadline(String projectName, long taskId, LocalDate deadline) {
        Project project = projects.get(projectName);
        if (project == null) {
            throw new ProjectNotFoundException(projectName);
        }

        for (Task task : project.getTasks()) {
            if (task.getId() == taskId) {
                task.setDeadline(deadline);
                return;
            }
        }

        throw new TaskNotFoundException(taskId);
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
     * Reorganizes the entire task list into a chronological view grouped by deadline.
     * Instead of the standard hierarchy (Project -> Tasks), this builds an inverted
     * view model (Deadline -> Projects -> Tasks). It safely creates copies of the
     * projects and tasks so that the underlying domain state is not accidentally
     * modified by the UI rendering logic.
     *
     * @return a chronologically sorted list of deadline groups, with the
     * "no deadline" group appended at the very end.
     */
    public List<DeadlineGroup> getTasksGroupedByDeadline() {
        // TreeMap automatically sorts the groups by LocalDate chronologically
        java.util.Map<LocalDate, List<Project>> datedGroups = new java.util.TreeMap<>();
        List<Project> noDeadlineProjects = new java.util.ArrayList<>();

        for (Project sourceProject : projects.values()) {
            java.util.Map<LocalDate, Project> projectCopiesByDate = new java.util.LinkedHashMap<>();
            Project noDeadlineProject = new Project(sourceProject.getName());

            for (Task task : sourceProject.getTasks()) {
                if (task.getDeadline() == null) {
                    // Create a safe copy for tasks without a deadline
                    Task copiedTask = noDeadlineProject.addTask(task.getId(), task.getDescription());
                    copiedTask.setDone(task.isDone());
                    copiedTask.setDeadline(null);
                } else {
                    // Find or create a project bucket for this specific date
                    Project projectForDate = projectCopiesByDate.computeIfAbsent(
                            task.getDeadline(),
                            ignored -> new Project(sourceProject.getName())
                    );

                    // Create a safe copy for tasks with a deadline
                    Task copiedTask = projectForDate.addTask(task.getId(), task.getDescription());
                    copiedTask.setDone(task.isDone());
                    copiedTask.setDeadline(task.getDeadline());
                }
            }

            // Aggregate the daily project views into the master map
            for (Map.Entry<LocalDate, Project> entry : projectCopiesByDate.entrySet()) {
                datedGroups.computeIfAbsent(entry.getKey(), ignored -> new java.util.ArrayList<>())
                        .add(entry.getValue());
            }

            // Add the no-deadline project view if it contains any tasks
            if (!noDeadlineProject.getTasks().isEmpty()) {
                noDeadlineProjects.add(noDeadlineProject);
            }
        }

        // Convert the maps into the final List of View Models for the UI
        List<DeadlineGroup> result = new java.util.ArrayList<>();

        for (Map.Entry<LocalDate, List<Project>> entry : datedGroups.entrySet()) {
            result.add(new DeadlineGroup(entry.getKey(), entry.getValue()));
        }

        // Always put the tasks without deadlines at the very bottom of the view
        if (!noDeadlineProjects.isEmpty()) {
            result.add(new DeadlineGroup(null, noDeadlineProjects));
        }

        return result;
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
