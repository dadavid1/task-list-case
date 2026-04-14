package com.ortecfinance.tasklist;

import java.time.LocalDate;

/**
 * Represents a single task that needs to be completed.
 */
public final class Task {
    private final long id;
    private final String description;
    private boolean done;
    private LocalDate deadline;

    /**
     * Creates a new task. By default, the deadline is not set (null).
     *
     * @param id the unique ID of the task
     * @param description what the task is about
     * @param done whether the task is currently completed
     */
    public Task(long id, String description, boolean done) {
        this.id = id;
        this.description = description;
        this.done = done;
        this.deadline = null;
    }

    /**
     * Gets the unique task ID.
     *
     * @return the task ID
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the description of the task.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the task is completed.
     *
     * @return true if the task is done, false otherwise
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Updates the completion status of the task.
     *
     * @param done true to mark as completed, false to uncheck
     */
    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * Gets the deadline for the task.
     *
     * @return the deadline date, or null if no deadline is set
     */
    public LocalDate getDeadline() {
        return deadline;
    }

    /**
     * Sets a deadline for the task.
     *
     * @param deadline the date the task needs to be completed by
     */
    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
