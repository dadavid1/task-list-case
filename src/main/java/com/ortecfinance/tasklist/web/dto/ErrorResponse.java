package com.ortecfinance.tasklist.web.dto;

/**
 * DTO (Data Transfer Object) for representing API error responses.
 * This ensures that whenever the server encounters an error, the client
 * always receives a consistent JSON structure containing the details.
 *
 * @param message the user-friendly description of what went wrong
 */
public record ErrorResponse(String message) {
}
