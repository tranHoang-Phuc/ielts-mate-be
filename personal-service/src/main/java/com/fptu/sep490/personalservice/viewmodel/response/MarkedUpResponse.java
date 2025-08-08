package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record MarkedUpResponse(
        Map<UUID, Integer> markedUpIdsMapping
) {
}
