package com.fptu.sep490.personalservice.viewmodel.request;

import java.util.UUID;

public record MarkupCreationRequest(
        Integer markUpType,
        Integer taskType,
        Integer practiceType,
        UUID taskId
) {

}
