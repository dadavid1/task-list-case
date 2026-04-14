package com.ortecfinance.tasklist;

/**
 * Exception thrown when a requested task ID does not exst.
 */
public final class TaskNotFoundException extends RuntimeException {
    private final long taskId;

    /**
     * Creates a new exception for a missing task.
     *
     * @param taskId the ID of the task that could not be found
     */
    public TaskNotFoundException(long taskId) {
        super(String.format("Could not find a task with an ID of %d.", taskId));
        this.taskId = taskId;
    }

    /**
     * Gets the ID of the missing task
     *
     * @return the missing task ID
     */
    public long getTaskId() {
        return taskId;
    }
}
