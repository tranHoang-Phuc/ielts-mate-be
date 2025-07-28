package com.fptu.sep490.personalservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShareModuleRequest(
        @JsonProperty("users")
        java.util.List<String> users
) {
}
