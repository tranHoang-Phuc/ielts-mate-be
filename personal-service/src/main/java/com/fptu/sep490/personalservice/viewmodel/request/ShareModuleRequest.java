package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ShareModuleRequest(
        @JsonProperty("users")
        java.util.List<String> users
) {
}
