package com.fptu.sep490.apigateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify WebSocket configuration
 */
@RestController
@RequestMapping("/api/test")
public class WebSocketTestController {

    @GetMapping("/websocket-config")
    public ResponseEntity<Map<String, Object>> testWebSocketConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "WebSocket configuration is active");
        response.put("webSocketPath", "/api/v1/personal/ws");
        response.put("gatewayInfo", "Spring Cloud Gateway with WebSocket support enabled");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
