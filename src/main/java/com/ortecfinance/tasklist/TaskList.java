package com.ortecfinance.tasklist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

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
                add(commandRest[1]);
                break;
            case "check":
                check(commandRest[1]);
                break;
            case "uncheck":
                uncheck(commandRest[1]);
                break;
            case "help":
                help();
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
        for (Project project : service.getProjects()) {
            out.println(project.getName());
            for (Task task : project.getTasks()) {
                out.printf("    [%c] %d: %s%n", (task.isDone() ? 'x' : ' '), task.getId(), task.getDescription());
            }
            out.println();
        }
    }

    /**
     * Parses an 'add' command and routes it to create either a project or a task.
     *
     * @param commandLine the remaining part of the command after 'add '
     */
    private void add(String commandLine) {
        String[] subcommandRest = commandLine.split(" ", 2);
        String subcommand = subcommandRest[0];
        if (subcommand.equals("project")) {
            addProject(subcommandRest[1]);
        } else if (subcommand.equals("task")) {
            String[] projectTask = subcommandRest[1].split(" ", 2);
            addTask(projectTask[0], projectTask[1]);
        }
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
     * @param project     the target project name
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
     * @param idString the ID of the task to check off
     */
    private void check(String idString) {
        setDone(idString, true);
    }

    /**
     * Marks a task as not complete.
     *
     * @param idString the ID of the task to uncheck
     */
    private void uncheck(String idString) {
        setDone(idString, false);
    }

    /**
     * Helper method to parse a task ID and update its completion status.
     *
     * @param idString the ID of the task as a string
     * @param done     true to check, false to uncheck
     */
    private void setDone(String idString, boolean done) {
        int id = Integer.parseInt(idString);
        try {
            if (done) {
                service.checkTask(id);
            } else {
                service.uncheckTask(id);
            }
        } catch (TaskNotFoundException e) {
            out.printf("Could not find a task with an ID of %d.", e.getTaskId());
            out.println();
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
        out.println();
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
}
