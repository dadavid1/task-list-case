package com.ortecfinance.tasklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortecfinance.tasklist.service.TaskListService;
import com.ortecfinance.tasklist.web.dto.CreateProjectRequest;
import com.ortecfinance.tasklist.web.dto.CreateTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Integration tests for the ProjectController.
 * This spins up the Spring context and simulates real HTTP requests
 * to ensure the web layer and the core service interact correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Resets the service state after each test
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskListService service;

    /**
     * Verifies that POST /projects creates a new project.
     *
     * This test checks both:
     * 1. the HTTP behavior: the endpoint returns 201 Created
     * 2. the application behavior: the project is actually stored in the service
     */
    @Test
    void it_creates_a_project() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("secrets");

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertEquals(1, service.getProjects().size());
        assertEquals("secrets", service.getProjects().getFirst().getName());
        assertEquals(0, service.getProjects().getFirst().getTasks().size());
    }

    /**
     * Verifies that GET /projects returns an empty list when no projects exist yet.
     */
    @Test
    void it_returns_an_empty_project_list_when_no_projects_exist() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Verifies that a newly created project is returned by GET /projects.
     */
    @Test
    void it_returns_created_projects() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("secrets");

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("secrets"))
                .andExpect(jsonPath("$[0].tasks").isArray())
                .andExpect(jsonPath("$[0].tasks.length()").value(0));
    }

    /**
     * Verifies that GET /projects returns the full project/task structure.
     */
    @Test
    void it_returns_all_projects_with_their_tasks() throws Exception {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        service.addProject("training");
        service.addTask("training", "SOLID");
        service.setDeadline(2, LocalDate.of(2024, 11, 25));

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$[0].name").value("secrets"))
                .andExpect(jsonPath("$[0].tasks[0].id").value(1))
                .andExpect(jsonPath("$[0].tasks[0].description").value("Eat more donuts."))
                .andExpect(jsonPath("$[0].tasks[0].done").value(false))
                .andExpect(jsonPath("$[0].tasks[0].deadline").value(nullValue()))

                .andExpect(jsonPath("$[1].name").value("training"))
                .andExpect(jsonPath("$[1].tasks[0].id").value(2))
                .andExpect(jsonPath("$[1].tasks[0].description").value("SOLID"))
                .andExpect(jsonPath("$[1].tasks[0].done").value(false))
                .andExpect(jsonPath("$[1].tasks[0].deadline").value("25-11-2024"));
    }

    /**
     * Verifies that POST /projects/{projectName}/tasks creates a task inside an existing project.
     */
    @Test
    void it_creates_a_task_for_a_project() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("training"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects/{projectName}/tasks", "training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTaskRequest("SOLID"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("training"))
                .andExpect(jsonPath("$[0].tasks.length()").value(1))
                .andExpect(jsonPath("$[0].tasks[0].id").value(1))
                .andExpect(jsonPath("$[0].tasks[0].description").value("SOLID"))
                .andExpect(jsonPath("$[0].tasks[0].done").value(false))
                .andExpect(jsonPath("$[0].tasks[0].deadline").value(nullValue()));
    }

    /**
     * Verifies that creating a task for a missing project returns a clear 404 response
     * instead of exposing an internal server error.
     */
    @Test
    void it_returns_not_found_when_creating_a_task_for_a_missing_project() throws Exception {
        mockMvc.perform(post("/projects/{projectName}/tasks", "missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTaskRequest("SOLID"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Could not find a project with the name \"missing\"."));
    }

    /**
     * Verifies that PUT /projects/{projectName}/tasks/{taskId}?deadline=...
     * updates the deadline of an existing task successfully.
     */
    @Test
    void it_updates_the_deadline_of_a_task() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("training"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects/{projectName}/tasks", "training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTaskRequest("SOLID"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/projects/{projectName}/tasks/{taskId}", "training", 1)
                        .param("deadline", "25-11-2024"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tasks[0].deadline").value("25-11-2024"));
    }

    /**
     * Verifies that updating a deadline for a task inside a project that does not exist
     * returns a 404 response.
     */
    @Test
    void it_returns_not_found_when_updating_a_deadline_for_a_missing_project() throws Exception {
        mockMvc.perform(put("/projects/{projectName}/tasks/{taskId}", "missing", 1)
                        .param("deadline", "25-11-2024"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Could not find a project with the name \"missing\"."));
    }

    /**
     * Verifies that updating a deadline for a task that does not exist
     * inside the requested project returns a 404 response.
     */
    @Test
    void it_returns_not_found_when_updating_a_missing_task() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("training"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/projects/{projectName}/tasks/{taskId}", "training", 99)
                        .param("deadline", "25-11-2024"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Could not find a task with an ID of 99."));
    }

    /**
     * Verifies that using the wrong date format returns 400 Bad Request.
     */
    @Test
    void it_returns_bad_request_for_an_invalid_deadline_format() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("training"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects/{projectName}/tasks", "training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTaskRequest("SOLID"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/projects/{projectName}/tasks/{taskId}", "training", 1)
                        .param("deadline", "2024-11-25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date. Please use format dd-MM-yyyy."));
    }

    /**
     * Verifies that GET /projects/view_by_deadline returns tasks grouped
     * chronologically by deadline, with the no-deadline group placed last.
     */
    @Test
    void it_returns_tasks_grouped_by_deadline() throws Exception {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");
        service.addTask("secrets", "Destroy all humans.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        service.setDeadline("secrets", 1, LocalDate.of(2024, 11, 11));
        service.setDeadline("training", 3, LocalDate.of(2024, 11, 13));

        mockMvc.perform(get("/projects/view_by_deadline"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$[0].deadline").value("11-11-2024"))
                .andExpect(jsonPath("$[0].projects[0].name").value("secrets"))
                .andExpect(jsonPath("$[0].projects[0].tasks[0].id").value(1))
                .andExpect(jsonPath("$[0].projects[0].tasks[0].description").value("Eat more donuts."))

                .andExpect(jsonPath("$[1].deadline").value("13-11-2024"))
                .andExpect(jsonPath("$[1].projects[0].name").value("training"))
                .andExpect(jsonPath("$[1].projects[0].tasks[0].id").value(3))
                .andExpect(jsonPath("$[1].projects[0].tasks[0].description").value("SOLID"))

                .andExpect(jsonPath("$[2].deadline").value(nullValue()))
                .andExpect(jsonPath("$[2].projects[0].name").value("secrets"))
                .andExpect(jsonPath("$[2].projects[0].tasks[0].id").value(2))
                .andExpect(jsonPath("$[2].projects[0].tasks[0].description").value("Destroy all humans."));
    }

    /**
     * Verifies that tasks with the same deadline remain grouped by their projects.
     */
    @Test
    void it_groups_tasks_by_project_inside_the_same_deadline() throws Exception {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts.");

        service.addProject("training");
        service.addTask("training", "SOLID");

        service.setDeadline("secrets", 1, LocalDate.of(2024, 11, 25));
        service.setDeadline("training", 2, LocalDate.of(2024, 11, 25));

        mockMvc.perform(get("/projects/view_by_deadline"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$[0].deadline").value("25-11-2024"))
                .andExpect(jsonPath("$[0].projects.length()").value(2))

                .andExpect(jsonPath("$[0].projects[0].name").value("secrets"))
                .andExpect(jsonPath("$[0].projects[0].tasks[0].id").value(1))
                .andExpect(jsonPath("$[0].projects[0].tasks[0].description").value("Eat more donuts."))

                .andExpect(jsonPath("$[0].projects[1].name").value("training"))
                .andExpect(jsonPath("$[0].projects[1].tasks[0].id").value(2))
                .andExpect(jsonPath("$[0].projects[1].tasks[0].description").value("SOLID"));
    }

    /**
     * Verifies that the deadline view returns an empty list when there are no tasks.
     */
    @Test
    void it_returns_an_empty_deadline_view_when_there_are_no_tasks() throws Exception {
        mockMvc.perform(get("/projects/view_by_deadline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
