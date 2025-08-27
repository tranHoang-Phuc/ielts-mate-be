package com.fptu.sep490.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for WebSocket routes in Spring Cloud Gateway
 * This ensures WebSocket connections are properly routed to downstream services
 */
@Configuration
public class WebSocketRoutesConfig {

    /**
     * Configure WebSocket-specific routes programmatically
     * This provides more control over WebSocket routing behavior
     */
    @Bean
    public RouteLocator webSocketRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // WebSocket route for personal service (attempt sessions)
                .route("personal-websocket-programmatic", r -> r
                        .path("/api/v1/personal/ws/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(new WebSocketProxyConfig().apply(new WebSocketProxyConfig.Config()))
                        )
                        .uri("${PERSONAL_API:http://localhost:8072}")
                )
                .build();
    }
}
