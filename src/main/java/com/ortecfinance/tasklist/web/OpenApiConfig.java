package com.ortecfinance.tasklist.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides basic OpenAPI metadata for the generated Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Customizes the generated OpenAPI documentation with a title,
     * description, and version.
     *
     * @return the OpenAPI metadata used by Swagger UI
     */
    @Bean
    public OpenAPI taskListOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task List API")
                        .description("REST API for managing projects, tasks, and deadlines.")
                        .version("1.0.0"));
    }
}
