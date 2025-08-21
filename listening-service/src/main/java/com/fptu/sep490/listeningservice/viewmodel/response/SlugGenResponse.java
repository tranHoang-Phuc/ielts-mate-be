package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SlugGenResponse(
        @JsonProperty("url_slug")
        String urlSlug
) {
}
