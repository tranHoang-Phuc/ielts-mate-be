package com.fptu.sep490.listeningservice.repository.client;

import com.fptu.sep490.listeningservice.viewmodel.request.GenTranscriptRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.TranscriptGenResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.TranscriptResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "assembly-ai-client", url = "${assembly-ai.url}")
public interface AssemblyAIClient {
     @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
     ResponseEntity<TranscriptGenResponse> createGenTranscriptRequest(
            @RequestBody GenTranscriptRequest genTranscriptRequest,
            @RequestHeader("authorization") String apiKey);

    @GetMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TranscriptResponse> getTranscript(
            @PathVariable("id") UUID id,
            @RequestHeader("authorization") String apiKey);
}
