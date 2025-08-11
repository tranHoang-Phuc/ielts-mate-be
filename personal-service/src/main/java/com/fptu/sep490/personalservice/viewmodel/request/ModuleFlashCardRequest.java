package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.personalservice.model.enumeration.LearningStatus;

public record ModuleFlashCardRequest(
        @JsonProperty("learning_status")
        LearningStatus learningStatus // Learning status of the flashcard, e.g., NEW, LEARNING, REVIEW
){

}
