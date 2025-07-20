package com.fptu.sep490.personalservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Personal Service API", version = "v1"),
        servers = {
                @Server(url = "/api/v1/personal", description = "Personal Service")
        }
)
public class OpenApiConfig {
}
