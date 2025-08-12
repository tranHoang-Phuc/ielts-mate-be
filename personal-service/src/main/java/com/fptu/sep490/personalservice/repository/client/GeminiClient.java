package com.fptu.sep490.personalservice.repository.client;

import com.fptu.sep490.personalservice.viewmodel.request.GeminiRequest;
import com.fptu.sep490.personalservice.viewmodel.response.GeminiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "gemini-client",
        url = "${gemini.api.url}"
)
public interface GeminiClient {
    @PostMapping(
            value = "/v1beta/models/gemini-2.0-flash:generateContent",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<GeminiResponse> callModel20Flash(
            @RequestBody GeminiRequest request,
            @RequestHeader("X-goog-api-key") String apiKey
    );
}
