package com.ortecfinance.tasklist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * The console-based user interface for the application.
 * It reads commands from the user, routes them to the core service,
 * and prints the results back to the screen.
 */
public final class TaskList implements Runnable {
    private static final String QUIT = "quit";

    private final BufferedReader in;
    private final PrintWriter out;

    private final TaskListService service;

    /**
     * Date formatter used to parse deadlines.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Starts the application in console mode using standard system streams.
     */
    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        new TaskList(in, out).run();
    }

    /**
     * Creates a new TaskList interface with a default underlying service.
     *
     * @param reader the input stream to read commands from
     * @param writer the output stream to print results to
     */
    public TaskList(BufferedReader reader, PrintWriter writer) {
        this(reader, writer, new TaskListService());
    }

    /**
     * Creates a new TaskList interface with a specific service.
     * This constructor allows for dependency injection, making it easy to test.
     *
     * @param reader  the input stream to read commands from
     * @param writer  the output stream to print results to
     * @param service the core business logic engine
     */
    TaskList(BufferedReader reader, PrintWriter writer, TaskListService service) {
        this.in = reader;
        this.out = writer;
        this.service = service;
    }

    /**
     * Starts the main application loop. Continues reading and executing
     * commands until the user types 'quit'.
     */
    @Override
    public void run() {
        out.println("Welcome to TaskList! Type 'help' for available commands.");
        while (true) {
            out.print("> ");
            out.flush();
            String command;
            try {
                command = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (command.equals(QUIT)) {
                break;
            }
            execute(command);
        }
    }

    /**
     * Parses the raw input string and routes it to the appropriate command method.
     *
     * @param commandLine the full text entered by the user
     */
    private void execute(String commandLine) {
        String[] commandRest = commandLine.split(" ", 2);
        String command = commandRest[0];
        switch (command) {
            case "show":
                show();
                break;
            case "add":
                if (commandRest.length < 2 || commandRest[1].isBlank()) {
                    printAddUsage();
                    return;
                }
                add(commandRest[1]);
                break;
            case "check":
                check(commandRest.length < 2 ? "" : commandRest[1]);
                break;
            case "uncheck":
                uncheck(commandRest.length < 2 ? "" : commandRest[1]);
                break;
            case "help":
                help();
                break;
            case "deadline":
                deadline(commandRest.length < 2 ? "" : commandRest[1]);
                break;
            case "view-by-deadline":
                viewByDeadline();
                break;
            case "today":
                today();
                break;
            default:
                error(command);
                break;
        }
    }

    /**
     * Retrieves all projects and tasks from the service and prints them to the console.
     */
    private void show() {
        showProjects(service.getProjects());
    }

    /**
     * A reusable helper method that formats and prints a specific list of projects to the console.
     * By accepting a list parameter, this method can render both the full project list
     * and filtered views (such as tasks due today).
     *
     * @param projects the list of projects to display
     */
    private void showProjects(java.util.List<Project> projects) {
        for (Project project : projects) {
            out.println(project.getName());
            for (Task task : project.getTasks()) {
                out.printf("    [%c] %d: %s%n",
                        task.isDone() ? 'x' : ' ',
                        task.getId(),
                        task.getDescription());
            }
            out.println();
        }
    }

    /**
     * Parses an 'add' command and routes it to create either a project or a task.
     * Validates the input to ensure all required arguments are provided.
     * Prints specific correct usage instructions if the arguments are missing or incorrect.
     *
     * @param commandLine the remaining part of the command after 'add '
     */
    private void add(String commandLine) {
        String[] parts = commandLine.split(" ", 3);
        String subcommand = parts[0];

        // Case of adding a project
        if (subcommand.equals("project")) {
            if (parts.length != 2 || parts[1].isBlank()) {
                out.println("Please don't forget the correct usage: add project <project name>");
                out.println("Please set <project name> as one word only.");
                return;
            }

            addProject(parts[1]);
            return;
        }

        // Case of adding a task
        if (subcommand.equals("task")) {
            if (parts.length < 3 || parts[1].isBlank() || parts[2].isBlank()) {
                out.println("Please don't forget the correct usage: add task <project name> <task description>");
                return;
            }

            addTask(parts[1], parts[2]);
            return;
        }

        // Case of adding something unknown (not a project and not a task)
        out.printf("I don't know how to add \"%s\".%n", subcommand);
    }

    /**
     * Adds a new project.
     *
     * @param name the name of the new project
     */
    private void addProject(String name) {
        service.addProject(name);
    }

    /**
     * Adds a new task to an existing project.
     *
     * @param project the target project name
     * @param description what the task is about
     */
    private void addTask(String project, String description) {
        try {
            service.addTask(project, description);
        } catch (ProjectNotFoundException e) {
            out.printf("Could not find a project with the name \"%s\".", e.getProjectName());
            out.println();
        }
    }

    /**
     * Marks a task as complete.
     *
     * @param commandLine the remaining part of the command containing the task ID
     */
    private void check(String commandLine) {
        setDone("check", commandLine, true);
    }

    /**
     * Marks a task as not complete.
     *
     * @param commandLine the remaining part of the command containing the task ID
     */
    private void uncheck(String commandLine) {
        setDone("uncheck", commandLine, false);
    }

    /**
     * Helper method to validate the input for check/uncheck commands and update the completion
     * status of a task.
     * Provides clear usage instructions if the input is missing, contains too many arguments,
     * or is not a valid numeric ID.
     *
     * @param commandName the name of the command invoked (used for formatting usage messages)
     * @param commandLine the raw input string expected to contain exactly one target task ID
     * @param done true to check, false to uncheck
     */
    private void setDone(String commandName, String commandLine, boolean done) {
        String[] parts = commandLine.trim().split("\\s+");

        if (commandLine.isBlank() || parts.length != 1) {
            out.printf("Please don't forget the correct usage: %s <task ID>%n", commandName);
            return;
        }

        long id;
        try {
            id = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            out.printf("Invalid task ID \"%s\". Please provide a numeric ID.%n", parts[0]);
            return;
        }

        try {
            if (done) {
                service.checkTask(id);
            } else {
                service.uncheckTask(id);
            }
        } catch (TaskNotFoundException e) {
            out.printf("Could not find a task with an ID of %d.%n", e.getTaskId());
        }
    }

    /**
     * Prints the list of available commands.
     */
    private void help() {
        out.println("Commands:");
        out.println("  show");
        out.println("  add project <project name>");
        out.println("  add task <project name> <task description>");
        out.println("  check <task ID>");
        out.println("  uncheck <task ID>");
        out.println("  deadline <task ID> <date>");
        out.println("  view-by-deadline");
        out.println("  today");
        out.println();
    }

    /**
     * Parses the 'deadline' command to assign a due date to a specific task.
     * Expects the input string to contain a task ID and a date formatted as dd-MM-yyyy.
     * Hnadles missing or incomplete arguments, non-numeric task IDs, invalid date formats,
     * and non-existent task IDs.
     *
     * @param commandLine the remaining part of the command containing the ID and date
     */
    private void deadline(String commandLine) {
        String[] parts = commandLine.split(" ", 3);

        if (commandLine.isBlank() || parts.length != 2) {
            out.println("Please don't forget the correct usage: deadline <task ID> <date>");
            return;
        }

        long taskId;
        try {
            taskId = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            out.printf("Invalid task ID \"%s\". Please provide a numeric ID.%n", parts[0]);
            return;
        }

        try {
            LocalDate deadline = LocalDate.parse(parts[1], DATE_FORMATTER);
            service.setDeadline(taskId, deadline);
        } catch (DateTimeParseException e) {
            out.printf("Invalid date \"%s\". Please use format dd-MM-yyyy.%n", parts[1]);
        } catch (TaskNotFoundException e) {
            out.printf("Could not find a task with an ID of %d.%n", e.getTaskId());
        }
    }

    /**
     * Handles the 'view by deadline' command.
     * Displays all tasks in the application grouped chronologically by their assigned deadlines.
     * <p>
     * Uses the DeadlineGroup view model to easily distinguish between tasks
     * that have specific due dates and tasks that have no deadline assigned.
     */
    private void viewByDeadline() {
        for (DeadlineGroup group : service.getTasksGroupedByDeadline()) {
            // Print the group header (either the formatted date or the fallback text)
            if (group.isNoDeadlineGroup()) {
                out.println("No deadline:");
            } else {
                out.printf("%s:%n", group.getDeadline().format(DATE_FORMATTER));
            }

            // Iterate through the projects in the deadline group and print the tasks
            for (Project project : group.getProjects()) {
                out.printf("    %s:%n", project.getName());
                for (Task task : project.getTasks()) {
                    out.printf("        %d: %s%n", task.getId(), task.getDescription());
                }
            }
        }
    }

    /**
     * Retrieves and displays all projects containing tasks that are due exactly today.
     * Relies on the underlying service to handle the time-based filtering logic.
     */
    private void today() {
        showProjects(service.getProjectsWithTasksDueToday());
    }

    /**
     * Prints an error message for unrecognized commands.
     *
     * @param command the invalid command text
     */
    private void error(String command) {
        out.printf("I don't know what the command \"%s\" is.", command);
        out.println();
    }

    /**
     * Prints a message with the usage details for the 'add' command.
     */
    private void printAddUsage() {
        out.println("I don't understand that. Don't forget the correct usage of add:");
        out.println("1st usage: add project <project name>");
        out.println("2nd usage: add task <project name> <task description>");
    }
}
