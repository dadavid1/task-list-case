package com.ortecfinance.tasklist.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A data structure that groups a list of projects by a shared deadline.
 * This is primarily used as a view model to make rendering the UI simpler.
 */
public final class DeadlineGroup {
    private final LocalDate deadline;
    private final List<Project> projects;

    /**
     * Creates a new group of projects associated with a specific deadline.
     *
     * @param deadline the shared deadline for this group (can be null for tasks without a deadline)
     * @param projects the list of projects containing tasks due on this date
     */
    public DeadlineGroup(LocalDate deadline, List<Project> projects) {
        this.deadline = deadline;
        this.projects = projects;
    }

    /**
     * Gets the deadline for this group.
     *
     * @return the deadline date, or null if this group represents tasks with no deadline
     */
    public LocalDate getDeadline() {
        return deadline;
    }

    /**
     * Gets the list of projects belonging to this deadline group.
     *
     * @return the list of projects
     */
    public List<Project> getProjects() {
        return projects;
    }

    /**
     * A helper method to determine if this group is the special bucket
     * for tasks that do not have a deadline assigned.
     *
     * @return true if there is no deadline, false otherwise
     */
    public boolean isNoDeadlineGroup() {
        return deadline == null;
    }
}
