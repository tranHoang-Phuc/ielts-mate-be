package com.fptu.sep490.readingservice.viewmodel.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MarkedUpResponse(
        Map<UUID, Integer> markedUpIdsMapping
) {
}
