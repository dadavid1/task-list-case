package com.ortecfinance.tasklist.dto;

/**
 * DTO (Data Transfer Object) for incoming requests to create a new project.
 *
 * @param name the name of the project to be created
 */
public record CreateProjectRequest(String name) {
}
