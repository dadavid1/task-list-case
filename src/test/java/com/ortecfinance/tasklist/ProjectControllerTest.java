package com.ortecfinance.tasklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import com.ortecfinance.tasklist.dto.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
