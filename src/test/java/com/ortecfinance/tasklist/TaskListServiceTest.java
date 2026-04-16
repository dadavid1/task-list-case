package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import com.ortecfinance.tasklist.model.DeadlineGroup;
import com.ortecfinance.tasklist.model.Project;
import com.ortecfinance.tasklist.model.Task;
import com.ortecfinance.tasklist.service.TaskListService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the core business logic (TaskListService).
 */
public final class TaskListServiceTest {

    /**
     * Verifies that projects and tasks are stored and retrieved in the exact order
     * they were created. This ensures predictable output for the console and API.
     */
    @Test
    void it_adds_projects_and_tasks_in_insertion_order() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        service.addTask("secrets", "Destroy all humans.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        List<Project> projects = service.getProjects();

        assertEquals(2, projects.size());

        assertEquals("secrets", projects.get(0).getName());
        assertEquals(2, projects.get(0).getTasks().size());
        assertEquals(1L, projects.get(0).getTasks().get(0).getId());
        assertEquals("Eat more donuts.", projects.get(0).getTasks().get(0).getDescription());
        assertEquals(2L, projects.get(0).getTasks().get(1).getId());

        assertEquals("training", projects.get(1).getName());
        assertEquals(1, projects.get(1).getTasks().size());
        assertEquals(3L, projects.get(1).getTasks().get(0).getId());
        assertEquals("SOLID", projects.get(1).getTasks().get(0).getDescription());
    }

    /**
     * Verifies that a task's completion status can be successfully toggled
     * back and forth using its unique ID.
     */
    @Test
    void it_can_check_and_uncheck_a_task() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        Task task = service.getProjects().getFirst().getTasks().getFirst();
        assertFalse(task.isDone());

        service.checkTask(task.getId());
        assertTrue(task.isDone());

        service.uncheckTask(task.getId());
        assertFalse(task.isDone());
    }

    /**
     * Ensures that modifying the state of one specific task does not cause
     * unintended side effects on other tasks within the same project.
     */
    @Test
    void checking_one_task_does_not_change_other_tasks() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        service.addTask("secrets", "Destroy all humans.");

        Project project = service.getProjects().getFirst();
        Task firstTask = project.getTasks().get(0);
        Task secondTask = project.getTasks().get(1);

        service.checkTask(firstTask.getId());

        assertTrue(firstTask.isDone());
        assertFalse(secondTask.isDone());
    }

    /**
     * Confirms that the service throws the appropriate exceptions
     * when interacting with non-existent projects or tasks.
     */
    @Test
    void it_throws_when_project_or_task_does_not_exist() {
        TaskListService service = new TaskListService();

        ProjectNotFoundException projectException =
                assertThrows(ProjectNotFoundException.class,
                        () -> service.addTask("missing", "Task"));

        assertEquals("missing", projectException.getProjectName());

        TaskNotFoundException taskException =
                assertThrows(TaskNotFoundException.class,
                        () -> service.checkTask(99));

        assertEquals(99L, taskException.getTaskId());
    }

    /**
     * Validates encapsulation. Ensures that external callers cannot directly modify
     * the internal data structures of the service or its underlying objects.
     */
    @Test
    void returned_collections_cannot_be_modified_from_outside() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        List<Project> projects = service.getProjects();
        Project project = projects.getFirst();

        assertThrows(UnsupportedOperationException.class,
                () -> projects.add(new Project("training")));

        assertThrows(UnsupportedOperationException.class,
                () -> project.getTasks().add(new Task(99, "Injected task", false)));
    }

    /**
     * Verifies that a task can be successfully updated with a deadline.
     * Also checks that newly created tasks default to having no
     * deadline.
     */
    @Test
    void it_can_set_a_deadline_for_a_task() {
        TaskListService service = new TaskListService();

        service.addProject("training");
        service.addTask("training", "SOLID");

        Task task = service.getProjects().getFirst().getTasks().getFirst();
        assertNull(task.getDeadline());

        service.setDeadline(task.getId(), java.time.LocalDate.of(2024, 11, 25));

        assertEquals(java.time.LocalDate.of(2024, 11, 25), task.getDeadline());
    }

    /**
     * Verifies that a deadline can be assigned when the task belongs
     * to the project specified by the caller.
     */
    @Test
    void it_can_set_a_deadline_for_a_task_inside_a_specific_project() {
        TaskListService service = new TaskListService();

        service.addProject("training");
        service.addTask("training", "SOLID");

        service.setDeadline("training", 1, java.time.LocalDate.of(2024, 11, 25));

        Task task = service.getProjects().getFirst().getTasks().getFirst();
        assertEquals(java.time.LocalDate.of(2024, 11, 25), task.getDeadline());
    }

    /**
     * Verifies that the service does not update a task if the task ID exists,
     * but belongs to a different project than the one specified.
     */
    @Test
    void it_throws_when_setting_deadline_for_task_outside_given_project() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        TaskNotFoundException exception =
                assertThrows(TaskNotFoundException.class,
                        () -> service.setDeadline("training", 1, java.time.LocalDate.of(2024, 11, 25)));

        assertEquals(1L, exception.getTaskId());
    }

    /**
     * Verifies the logic for filtering tasks that are specifically due today.
     * By injecting a fixed Clock, this test ensures the time-based filtering
     * is completely deterministic and will always pass regardless of the actual
     * date the test suite is executed on.
     */
    @Test
    void it_returns_only_tasks_due_today() {
        java.time.Clock fixedClock = java.time.Clock.fixed(
                java.time.Instant.parse("2024-11-25T10:15:30.00Z"),
                java.time.ZoneId.of("UTC")
        );

        // Injecting the mocked clock into the service
        TaskListService service = new TaskListService(fixedClock);

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        service.addTask("secrets", "Destroy all humans.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        service.setDeadline(1, java.time.LocalDate.of(2024, 11, 25)); // Due "today"
        service.setDeadline(3, java.time.LocalDate.of(2024, 11, 26)); // Due "tomorrow"

        List<Project> projectsDueToday = service.getProjectsWithTasksDueToday();

        assertEquals(1, projectsDueToday.size());
        assertEquals("secrets", projectsDueToday.getFirst().getName());
        assertEquals(1, projectsDueToday.getFirst().getTasks().size());
        assertEquals("Eat more donuts.", projectsDueToday.getFirst().getTasks().getFirst().getDescription());
    }

    /**
     * Ensures that if a project has no tasks due today, the entire project
     * is excluded from the filtered view.
     */
    @Test
    void it_excludes_projects_that_have_no_tasks_due_today() {
        java.time.Clock fixedClock = java.time.Clock.fixed(
                java.time.Instant.parse("2024-11-25T10:15:30Z"),
                java.time.ZoneId.of("UTC")
        );

        TaskListService service = new TaskListService(fixedClock);

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        service.setDeadline(1, java.time.LocalDate.of(2024, 11, 25));

        service.addProject("training");
        service.addTask("training", "SOLID");
        service.setDeadline(2, java.time.LocalDate.of(2024, 11, 26)); // Not due today

        List<Project> projects = service.getProjectsWithTasksDueToday();

        assertEquals(1, projects.size());
        assertEquals("secrets", projects.getFirst().getName());
    }

    /**
     * Verifies that tasks are correctly grouped chronologically by their deadlines,
     * and that the special "no deadline" group is always displayed at the bottom.
     */
    @Test
    void it_groups_tasks_by_deadline_and_puts_no_deadline_last() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        service.addTask("secrets", "Destroy all humans.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        service.setDeadline(1, java.time.LocalDate.of(2024, 11, 11));
        service.setDeadline(3, java.time.LocalDate.of(2024, 11, 13));

        List<DeadlineGroup> groups = service.getTasksGroupedByDeadline();

        assertEquals(3, groups.size());

        assertEquals(java.time.LocalDate.of(2024, 11, 11), groups.get(0).getDeadline());
        assertEquals("secrets", groups.get(0).getProjects().getFirst().getName());
        assertEquals("Eat more donuts.", groups.get(0).getProjects().getFirst().getTasks().getFirst().getDescription());

        assertEquals(java.time.LocalDate.of(2024, 11, 13), groups.get(1).getDeadline());
        assertEquals("training", groups.get(1).getProjects().getFirst().getName());

        assertNull(groups.get(2).getDeadline());
        assertEquals("secrets", groups.get(2).getProjects().getFirst().getName());
        assertEquals("Destroy all humans.", groups.get(2).getProjects().getFirst().getTasks().getFirst().getDescription());
    }

    /**
     * Ensures that if multiple tasks from entirely different projects share the
     * exact same deadline, they are successfully aggregated under a single date group.
     */
    @Test
    void it_groups_tasks_with_the_same_deadline_from_multiple_projects() {
        TaskListService service = new TaskListService();

        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        java.time.LocalDate sharedDeadline = java.time.LocalDate.of(2024, 11, 11);
        service.setDeadline(1, sharedDeadline);
        service.setDeadline(2, sharedDeadline);

        List<DeadlineGroup> groups = service.getTasksGroupedByDeadline();

        assertEquals(1, groups.size());
        assertEquals(sharedDeadline, groups.getFirst().getDeadline());

        List<Project> projects = groups.getFirst().getProjects();
        assertEquals(2, projects.size());

        assertEquals("secrets", projects.get(0).getName());
        assertEquals("Eat more donuts.", projects.get(0).getTasks().getFirst().getDescription());

        assertEquals("training", projects.get(1).getName());
        assertEquals("SOLID", projects.get(1).getTasks().getFirst().getDescription());
    }

    /**
     * Tests the edge case where every single task has a deadline assigned.
     * Ensures an empty "no deadline" group is not mistakenly generated.
     */
    @Test
    void it_does_not_create_a_no_deadline_group_when_all_tasks_have_deadlines() {
        TaskListService service = new TaskListService();

        service.addProject("training");
        service.addTask("training", "SOLID");
        service.addTask("training", "Coupling and Cohesion");

        service.setDeadline(1, java.time.LocalDate.of(2024, 11, 11));
        service.setDeadline(2, java.time.LocalDate.of(2024, 11, 13));

        List<DeadlineGroup> groups = service.getTasksGroupedByDeadline();

        assertEquals(2, groups.size());
        assertNotNull(groups.get(0).getDeadline());
        assertNotNull(groups.get(1).getDeadline());
    }

    /**
     * Tests the base case where the service has no tasks at all.
     * Ensures it returns an empty list rather than throwing a NullPointerException.
     */
    @Test
    void it_returns_no_deadline_groups_when_there_are_no_tasks() {
        TaskListService service = new TaskListService();

        List<DeadlineGroup> groups = service.getTasksGroupedByDeadline();

        assertTrue(groups.isEmpty());
    }
}
