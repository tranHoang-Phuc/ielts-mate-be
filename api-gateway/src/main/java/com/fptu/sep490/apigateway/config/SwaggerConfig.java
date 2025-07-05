package com.fptu.sep490.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {
//    private final DiscoveryClient discoveryClient;
//
//    public SwaggerConfig(DiscoveryClient discoveryClient) {
//        this.discoveryClient = discoveryClient;
//    }
//    @Bean
//    public List<GroupedOpenApi> apis() {
//        List<GroupedOpenApi> groups = new ArrayList<>();
//        discoveryClient.getServices().forEach(service -> {
//            groups.add(GroupedOpenApi.builder()
//                    .group(service)
//                    .pathsToMatch("/" + service.toLowerCase() + "/**")
//                    .build());
//        });
//        return groups;
//    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:9191/api/v1/identity").description("Identity Service"),
                        new Server().url("http://localhost:9191/api/v1/reading").description("Reading Service"),
                        new Server().url("http://localhost:9191/api/v1/resource").description("File Service"),
                        new Server().url("http://localhost:9191/api/v1/listening").description("Listening Service"),
                        new Server().url("http://localhost:9191/api/v1/notification").description("Notification Service")
                ))
                .info(new Info().title("SEP490").version("1.0.0"));
    }


}