package com.fptu.sep490.sample.viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductGetVm(
        @JsonProperty("id") long id,
        @JsonProperty("name") String name,
        @JsonProperty("short_description") String shortDescription
) {
}
