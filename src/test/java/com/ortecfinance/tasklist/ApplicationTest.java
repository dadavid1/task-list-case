package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.cli.TaskList;
import com.ortecfinance.tasklist.service.TaskListService;
import org.junit.jupiter.api.*;

import java.io.*;

import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public final class ApplicationTest {
    public static final String PROMPT = "> ";
    private final PipedOutputStream inStream = new PipedOutputStream();
    private final PrintWriter inWriter = new PrintWriter(inStream, true);

    private final PipedInputStream outStream = new PipedInputStream();
    private final BufferedReader outReader = new BufferedReader(new InputStreamReader(outStream));

    private Thread applicationThread;

    public ApplicationTest() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new PipedInputStream(inStream)));
        PrintWriter out = new PrintWriter(new PipedOutputStream(outStream), true);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2024-11-25T10:15:30Z"),
                ZoneId.of("UTC")
        );
        TaskListService service = new TaskListService(fixedClock);
        TaskList taskList = new TaskList(in, out, service);
        applicationThread = new Thread(taskList);
    }

    @BeforeEach
    public void start_the_application() throws IOException {
        applicationThread.start();
        readLines("Welcome to TaskList! Type 'help' for available commands.");
    }

    @AfterEach
    public void kill_the_application() throws IOException, InterruptedException {
        if (!stillRunning()) {
            return;
        }

        Thread.sleep(1000);
        if (!stillRunning()) {
            return;
        }

        applicationThread.interrupt();
        throw new IllegalStateException("The application is still running.");
    }

    @Test
    void it_works() throws IOException {
        execute("show");

        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("show");
        readLines(
            "secrets",
            "    [ ] 1: Eat more donuts.",
            "    [ ] 2: Destroy all humans.",
            ""
        );

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training SOLID");
        execute("add task training Coupling and Cohesion");
        execute("add task training Primitive Obsession");
        execute("add task training Outside-In TDD");
        execute("add task training Interaction-Driven Design");

        execute("check 1");
        execute("check 3");
        execute("check 5");
        execute("check 6");

        execute("show");
        readLines(
                "secrets",
                "    [x] 1: Eat more donuts.",
                "    [ ] 2: Destroy all humans.",
                "",
                "training",
                "    [x] 3: Four Elements of Simple Design",
                "    [ ] 4: SOLID",
                "    [x] 5: Coupling and Cohesion",
                "    [x] 6: Primitive Obsession",
                "    [ ] 7: Outside-In TDD",
                "    [ ] 8: Interaction-Driven Design",
                ""
        );

        execute("quit");
    }

    /**
     * Verifies that the 'help' command correctly displays the full list
     * of available commands to the user.
     */
    @Test
    void it_shows_help() throws IOException {
        execute("help");

        readLines(
                "Commands:",
                "  show",
                "  add project <project name>",
                "  add task <project name> <task description>",
                "  check <task ID>",
                "  uncheck <task ID>",
                "  deadline <task ID> <date>",
                "  view-by-deadline",
                "  today",
                ""
        );

        execute("quit");
    }

    /**
     * Ensures that the application handles unrecognized user input
     * by printing a helpful error message instead of crashing.
     */
    @Test
    void it_shows_error_for_unknown_command() throws IOException {
        execute("unknown-command");

        readLines(
                "I don't know what the command \"unknown-command\" is."
        );

        execute("quit");
    }

    /**
     * Ensures that the application handles the case of adding a task
     * to a project that has not been created yet by printing a helpful
     * error message.
     */
    @Test
    void it_shows_error_when_adding_task_to_missing_project() throws IOException {
        execute("add task missing Eat more donuts.");

        readLines(
                "Could not find a project with the name \"missing\"."
        );

        execute("quit");
    }

    /**
     * Ensures that the application handles the case of modifying a task
     * with an ID that does not exist by printing a helpful error message.
     */
    @Test
    void it_shows_an_error_when_checking_a_missing_task() throws IOException {
        execute("check 42");

        readLines(
                "Could not find a task with an ID of 42."
        );

        execute("quit");
    }

    /**
     * Verifies that the app prompts the user with the correct usage instructions
     * if they attempt to use the 'check' command without providing a target task ID.
     */
    @Test
    void it_shows_usage_when_check_has_no_id() throws IOException {
        execute("check");

        readLines(
                "Please don't forget the correct usage: check <task ID>"
        );

        execute("quit");
    }

    /**
     * Ensures the app warns the user when they provide letters or symbols instead of a
     * valid numeric task ID.
     */
    @Test
    void it_shows_an_error_when_check_id_is_not_numeric() throws IOException {
        execute("check abc");

        readLines(
                "Invalid task ID \"abc\". Please provide a numeric ID."
        );

        execute("quit");
    }

    /**
     * Checks that the app rejects the command and shows the proper syntax if a user
     * types additional, unexpected arguments after the task ID when checking it
     */
    @Test
    void it_shows_usage_when_check_has_too_many_arguments() throws IOException {
        execute("check 1 extra");

        readLines(
                "Please don't forget the correct usage: check <task ID>"
        );

        execute("quit");
    }

    /**
     * Verifies that the app prompts the user with the correct usage instructions
     * if they attempt to use the 'uncheck' command without providing a target task ID.
     */
    @Test
    void it_shows_usage_when_uncheck_has_no_id() throws IOException {
        execute("uncheck");

        readLines(
                "Please don't forget the correct usage: uncheck <task ID>"
        );

        execute("quit");
    }

    /**
     * Ensures the app warns the user when they provide letters or symbols instead of a
     * valid numeric task ID.
     */
    @Test
    void it_shows_an_error_when_uncheck_id_is_not_numeric() throws IOException {
        execute("uncheck abc");

        readLines(
                "Invalid task ID \"abc\". Please provide a numeric ID."
        );

        execute("quit");
    }

    /**
     * Checks that the app rejects the command and shows the proper syntax if a user
     * types additional, unexpected arguments after the task ID when unchecking it
     */
    @Test
    void it_shows_usage_when_uncheck_has_too_many_arguments() throws IOException {
        execute("uncheck 1 extra");

        readLines(
                "Please don't forget the correct usage: uncheck <task ID>"
        );

        execute("quit");
    }

    /**
     * Ensures that typing just 'add' without any additional arguments
     * prompts the user with the general usage guide for the add command.
     */
    @Test
    void it_shows_usage_when_add_has_no_arguments() throws IOException {
        execute("add");

        readLines(
                "I don't understand that. Don't forget the correct usage of add:",
                "1st usage: add project <project name>",
                "2nd usage: add task <project name> <task description>"
        );

        execute("quit");
    }

    /**
     * Verifies that the app politely reminds the user of the correct syntax
     * if they try to add a project but forget to type the project name.
     */
    @Test
    void it_shows_usage_when_add_project_has_no_project_name() throws IOException {
        execute("add project");

        readLines(
                "Please don't forget the correct usage: add project <project name>",
                "Please set <project name> as one word only."
        );

        execute("quit");
    }

    /**
     * Checks that the app displays the proper usage instructions if a user
     * starts adding a task but leaves out the required project name or
     * task description.
     */
    @Test
    void it_shows_usage_when_add_task_is_incomplete() throws IOException {
        execute("add task");

        readLines(
                "Please don't forget the correct usage: add task <project name> <task description>"
        );

        execute("add task training");

        readLines(
                "Please don't forget the correct usage: add task <project name> <task description>"
        );

        execute("quit");
    }

    /**
     * Ensures the app handles unrecognized 'add' subcommands
     * (like 'add random') by letting the user know it doesn't understand the input.
     */
    @Test
    void it_shows_an_error_for_an_unknown_add_subcommand() throws IOException {
        execute("add random");

        readLines(
                "I don't know how to add \"random\"."
        );

        execute("quit");
    }

    /**
     * Ensures that typing just 'deadline' without any arguments
     * shows the correct usage instructions.
     */
    @Test
    void it_shows_usage_when_deadline_has_no_arguments() throws IOException {
        execute("deadline");

        readLines(
                "Please don't forget the correct usage: deadline <task ID> <date>"
        );

        execute("quit");
    }

    /**
     * Verifies the app reminds the user to include the date if they
     * only type the command and the task ID.
     */
    @Test
    void it_shows_usage_when_deadline_has_missing_date() throws IOException {
        execute("deadline 1");

        readLines(
                "Please don't forget the correct usage: deadline <task ID> <date>"
        );

        execute("quit");
    }

    /**
     * Checks that providing letters instead of a number for the task ID
     * triggers a helpful error message.
     */
    @Test
    void it_shows_an_error_when_deadline_id_is_not_numeric() throws IOException {
        execute("deadline abc 25-11-2024");

        readLines(
                "Invalid task ID \"abc\". Please provide a numeric ID."
        );

        execute("quit");
    }

    /**
     * Ensures the app catches dates in the wrong format (like yyyy-MM-dd)
     * and reminds the user to use the dd-MM-yyyy format.
     */
    @Test
    void it_shows_an_error_when_deadline_date_has_invalid_format() throws IOException {
        execute("deadline 1 2024-11-25");

        readLines(
                "Invalid date \"2024-11-25\". Please use format dd-MM-yyyy."
        );

        execute("quit");
    }

    /**
     * Verifies the app rejects the command if the user accidentally
     * types extra words after the date.
     */
    @Test
    void it_shows_usage_when_deadline_has_too_many_arguments() throws IOException {
        execute("deadline 1 25-11-2024 extra");

        readLines(
                "Please don't forget the correct usage: deadline <task ID> <date>"
        );

        execute("quit");
    }

    /**
     * Ensures a friendly error is shown if the input is perfectly formatted,
     * but the task ID doesn't actually exist in the system.
     */
    @Test
    void it_shows_an_error_when_setting_deadline_for_a_missing_task() throws IOException {
        execute("deadline 42 25-11-2024");

        readLines(
                "Could not find a task with an ID of 42."
        );

        execute("quit");
    }

    /**
     * Verifies that the 'view-by-deadline' command prints tasks grouped
     * chronologically by deadline and places tasks without a deadline
     * in the final 'No deadline' group.
     */
    @Test
    void it_shows_tasks_grouped_by_deadline() throws IOException {
        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("add project training");
        execute("add task training SOLID");

        execute("deadline 1 11-11-2024");
        execute("deadline 3 13-11-2024");

        execute("view-by-deadline");

        readLines(
                "11-11-2024:",
                "    secrets:",
                "        1: Eat more donuts.",
                "13-11-2024:",
                "    training:",
                "        3: SOLID",
                "No deadline:",
                "    secrets:",
                "        2: Destroy all humans."
        );

        execute("quit");
    }

    /**
     * Verifies that the 'today' command prints only the tasks that have
     * a deadline equal to the current date, while preserving the same
     * checked/unchecked format used by the 'show' command.
     */
    @Test
    void it_shows_only_tasks_due_today() throws IOException {
        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("add project training");
        execute("add task training SOLID");

        execute("deadline 1 25-11-2024");
        execute("deadline 3 26-11-2024");
        execute("check 1");

        execute("today");

        readLines(
                "secrets",
                "    [x] 1: Eat more donuts.",
                ""
        );

        execute("quit");
    }

    private void execute(String command) throws IOException {
        read(PROMPT);
        write(command);
    }

    private void read(String expectedOutput) throws IOException {
        int length = expectedOutput.length();
        char[] buffer = new char[length];
        outReader.read(buffer, 0, length);
        assertThat(String.valueOf(buffer), is(expectedOutput));
    }

    private void readLines(String... expectedOutput) throws IOException {
        for (String line : expectedOutput) {
            read(line + lineSeparator());
        }
    }

    private void write(String input) {
        inWriter.println(input);
    }

    private boolean stillRunning() {
        return applicationThread != null && applicationThread.isAlive();
    }
}
