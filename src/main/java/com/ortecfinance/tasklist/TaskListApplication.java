package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.cli.TaskList;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the TaskList application.
 * Starts either the console interface or the REST API depending on the first argument.
 */
@SpringBootApplication
public class TaskListApplication {
    private static final String WEB_MODE = "web";

    /**
     * Starts the application.
     *
     * Without arguments, the console application is started.
     * With the argument "web", the Spring Boot REST API is started.
     *
     * @param args optional startup mode arguments
     */
    public static void main(String[] args) {
        // No arguments provided: start the traditional console interface
        if (args.length == 0) {
            System.out.println("Starting console application");
            TaskList.startConsole();
            return;
        }

        // "web" argument provided: start the Spring Boot REST API
        if (WEB_MODE.equalsIgnoreCase(args[0])) {
            SpringApplication.run(TaskListApplication.class, args);
            System.out.println("Web application started at http://localhost:8080/projects");
            System.out.println("Swagger UI available at http://localhost:8080/swagger-ui.html");
            return;
        }

        // Unrecognized argument provided: show the user how to start the app
        System.out.printf("Unknown startup mode \"%s\".%n", args[0]);
        System.out.println("Usage:");
        System.out.println("  no arguments -> start the console application");
        System.out.println("  web -> start the REST API");
    }
}
