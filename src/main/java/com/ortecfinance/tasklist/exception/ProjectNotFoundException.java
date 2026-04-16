package com.ortecfinance.tasklist.exception;

/**
 * Exception thrown when a requested project does not exist.
 */
public final class ProjectNotFoundException extends RuntimeException {
    private final String projectName;

    /**
     * Creates a new exception for a missing project.
     *
     * @param projectName the name of the project that could not be found
     */
    public ProjectNotFoundException(String projectName) {
        super(String.format("Could not find a project with the name \"%s\".", projectName));
        this.projectName = projectName;
    }

    /**
     * Gets the name of the missing project.
     *
     * @return the missing project name
     */
    public String getProjectName() {
        return projectName;
    }
}
