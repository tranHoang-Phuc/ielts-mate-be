package com.fptu.sep490.listeningservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record DragItemCreationRequest(@JsonProperty("content")
                                      String content) {
}
