package com.fptu.sep490.readingservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Identity Service API", version = "v1"),
        servers = {
                @Server(url = "/api/v1/reading", description = "Reading Service")
        }
)
public class OpenApiConfig {
}
