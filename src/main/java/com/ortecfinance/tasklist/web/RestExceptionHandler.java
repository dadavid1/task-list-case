package com.ortecfinance.tasklist.web;

import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import com.ortecfinance.tasklist.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;

/**
 * A global exception handler for the REST API.
 * This class intercepts specific Java exceptions thrown by the application
 * and translates them into clean, standardized HTTP error responses.
 */
@RestControllerAdvice
public final class RestExceptionHandler {

    /**
     * Handles cases where a requested project does not exist.
     * Translates the error into an HTTP 404 (Not Found) response.
     *
     * @param e the exception containing the missing project's name
     * @return an error response containing a user-friendly message
     */
    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleProjectNotFound(ProjectNotFoundException e) {
        return new ErrorResponse("Could not find a project with the name \"" + e.getProjectName() + "\".");
    }

    /**
     * Handles cases where a requested task ID does not exist.
     * Translates the error into an HTTP 404 (Not Found) response.
     *
     * @param e the exception containing the invalid task ID
     * @return an error response containing a user-friendly message
     */
    @ExceptionHandler(TaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTaskNotFound(TaskNotFoundException e) {
        return new ErrorResponse("Could not find a task with an ID of " + e.getTaskId() + ".");
    }

    /**
     * Handles cases where a user submits a deadline date in the wrong format.
     * Translates the error into an HTTP 400 (Bad Request) response.
     *
     * @param e the exception thrown when date parsing fails
     * @return an error response reminding the user of the correct dd-MM-yyyy format
     */
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidDate(DateTimeParseException e) {
        return new ErrorResponse("Invalid date. Please use format dd-MM-yyyy.");
    }
}
