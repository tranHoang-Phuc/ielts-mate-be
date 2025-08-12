package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.personalservice.model.enumeration.LearningStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record ModuleProgressResponse(
        @JsonProperty("id")
        String id,
        @JsonProperty("module_id")
        String moduleId,
        @JsonProperty("module_name")
        String moduleName,
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("time_spent")
        Long timeSpent, // in seconds
        @JsonProperty("status")
        Integer status, // 0: pending, 1: allowed, 2: denied
        @JsonProperty("progress")
        Double progress, // percentage of module completed
        @JsonProperty("learning_status")
        LearningStatus learningStatus, // 0: not started, 1: in progress, 2: completed
        @JsonProperty("attempts")
        Integer attempts, // number of attempts user has made on the module
        @JsonProperty("last_index_read")
        Integer lastIndexRead, // this is index that user last read in module, then when user open it, it will show the last read
        @JsonProperty("flashcard_progresses")
        List<FlashCardProgressResponse>  flashcardProgresses // List of flashcard progress responses

) {
}
