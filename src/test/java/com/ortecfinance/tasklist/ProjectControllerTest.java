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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
