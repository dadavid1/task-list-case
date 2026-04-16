package com.ortecfinance.tasklist.dto;

/**
 * DTO for incoming requests to add a new task to an existing project.
 *
 * @param description the description of the task to be created
 */
public record CreateTaskRequest(String description) {
}
