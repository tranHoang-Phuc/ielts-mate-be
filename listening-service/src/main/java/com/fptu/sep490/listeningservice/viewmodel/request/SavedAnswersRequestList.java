package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SavedAnswersRequestList(
        @JsonProperty("answers")
        List<SavedAnswersRequest> answers,
        @JsonProperty("duration")
        Long duration
) {
}
