package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserAttemptResponse(
        @JsonProperty("attempt_id")
        UUID attemptId,
        @JsonProperty("duration")
        Long duration,
        @JsonProperty("total_points")
        Integer totalPoints,
        @JsonProperty("status")
        Integer status,
        @JsonProperty("start_at")
        LocalDateTime startAt,
        @JsonProperty("finished_at")
        LocalDateTime finishedAt,
        @JsonProperty("listening_task_id")
        UUID listeningTaskId,
        @JsonProperty("title")
        String title

) {
}
