package com.fptu.sep490.identityservice.viewmodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeyCloakError(
        String errorMessage,
        String error,
        @JsonProperty("error_description")
        String errorDescription
) {
}
