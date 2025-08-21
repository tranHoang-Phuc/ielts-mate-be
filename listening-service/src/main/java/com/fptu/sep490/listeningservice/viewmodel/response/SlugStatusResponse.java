package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SlugStatusResponse(
        @JsonProperty("is_valid_slug")
        boolean isValid
) {
}
