package com.ortecfinance.tasklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A project that holds a list of tasks.
 */
public final class Project {

    /** The name of the project. */
    private final String name;

    /** The list of tasks in this project. */
    private final List<Task> tasks = new ArrayList<>();

    /**
     * Creates a new project.
     *
     * @param name the name of the project
     */
    public Project(String name) {
        this.name = name;
    }

    /**
     * Gets the project name.
     *
     * @return the project name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets all tasks in this project.
     * Note: This list is read-only and cannot be modified directly.
     *
     * @return a read-only list of tasks
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Adds a new, uncompleted task to the project.
     *
     * @param id the task ID
     * @param description what the task is about
     * @return the new task
     */
    public Task addTask(long id, String description) {
        Task task = new Task(id, description, false);
        tasks.add(task);
        return task;
    }
}
