package com.fptu.sep490.personalservice.repository.client;

import com.fptu.sep490.personalservice.viewmodel.request.OpenAIRequest;
import com.fptu.sep490.personalservice.viewmodel.response.OpenAIResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "openai-client", url = "${openai.api.url}")
public interface OpenAIClient {
    
    @PostMapping("/v1/chat/completions")
    OpenAIResponse callModel(@RequestBody OpenAIRequest request, 
                           @RequestHeader("Authorization") String authorization);
}
