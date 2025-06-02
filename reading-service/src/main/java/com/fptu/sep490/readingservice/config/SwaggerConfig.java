package com.fptu.sep490.readingservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(info = @Info(title = "File Services", description = "File API documentation", version = "1.0"),
        security = @SecurityRequirement(name = "oauth2_bearer"),
        servers = {@Server(url = "${server.servlet.context-path}", description = "Default Server URL")})
@SecurityScheme(name = "BearerAuth",
        type = SecuritySchemeType.HTTP, scheme = "Bearer", bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
public class SwaggerConfig {
}
