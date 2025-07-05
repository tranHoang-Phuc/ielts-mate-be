package com.fptu.sep490.fileservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "File Service API", version = "v1"),
        servers = {
                @Server(url = "/api/v1/resource", description = "File Service")
        }
)
public class OpenApiConfig {
}
