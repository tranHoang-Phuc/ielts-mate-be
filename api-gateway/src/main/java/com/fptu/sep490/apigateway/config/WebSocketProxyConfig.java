package com.fptu.sep490.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Custom Gateway Filter for WebSocket connections
 * Ensures proper headers are set for WebSocket upgrade requests
 */
@Component
public class WebSocketProxyConfig extends AbstractGatewayFilterFactory<WebSocketProxyConfig.Config> implements Ordered {

    public WebSocketProxyConfig() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerWebExchange.Builder builder = exchange.mutate();
            
            // Check if this is a WebSocket upgrade request
            if (isWebSocketUpgradeRequest(exchange)) {
                // Ensure required WebSocket headers are preserved
                builder.request(originalRequest -> originalRequest.headers(headers -> {
                    // Preserve WebSocket specific headers
                    if (!headers.containsKey("Upgrade")) {
                        headers.add("Upgrade", "websocket");
                    }
                    if (!headers.containsKey("Connection")) {
                        headers.add("Connection", "Upgrade");
                    }
                    
                    // Add headers for proper proxying
                    headers.add("X-Forwarded-Proto", exchange.getRequest().getURI().getScheme());
                    headers.add("X-Forwarded-Host", exchange.getRequest().getHeaders().getFirst("Host"));
                }));
            }
            
            return chain.filter(builder.build());
        };
    }
    
    private boolean isWebSocketUpgradeRequest(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return "websocket".equalsIgnoreCase(headers.getFirst("Upgrade")) &&
               headers.getConnection().contains("Upgrade");
    }

    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }

    public static class Config {
        // Configuration properties if needed
    }
}
