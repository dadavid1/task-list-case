package com.ortecfinance.tasklist.dto;

import java.util.List;

/**
 * DTO representing a project and its associated tasks sent in an API response.
 *
 * @param name the name of the project
 * @param tasks the list of formatted task responses belonging to this project
 */
public record ProjectResponse(String name, List<TaskResponse> tasks) {
}
