package com.demo.onboarding.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * Provides API documentation accessible at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Onboarding API")
                        .version("1.0.0")
                        .description("REST API for managing HR employee onboarding processes including CV creation tracking, " +
                                "car provisioning, and overall onboarding status management. This API provides comprehensive " +
                                "endpoints for creating employee records, tracking their onboarding progress, managing the " +
                                "company car fleet, and monitoring car assignments. The onboarding process is considered " +
                                "complete only when both CV creation and car provisioning are marked as completed.")
                        .contact(new Contact()
                                .name("HR IT Support")
                                .email("hr-it@company.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.company.com")
                                .description("Production server")
                ));
    }
}


