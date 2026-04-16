package com.ortecfinance.tasklist.web.dto;

/**
 * DTO representing a single task sent in an API response.
 * Formats the domain Task object into a safe, serializable JSON structure.
 *
 * @param id the unique identifier of the task
 * @param description what the task is about
 * @param done the current completion status of the task
 * @param deadline the formatted deadline string, or null if none
 */
public record TaskResponse(long id, String description, boolean done, String deadline) {
}
