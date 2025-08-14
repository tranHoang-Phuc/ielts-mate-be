package com.fptu.sep490.personalservice.viewmodel.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer n;
    private Boolean stream;

    @Data
    @Builder
    public static class Message {
        private String role; // "system", "user", "assistant"
        private String content;
    }
}
