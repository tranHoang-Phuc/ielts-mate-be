package com.fptu.sep490.listeningservice.viewmodel.response;

import java.util.Map;
import java.util.UUID;

public record MarkedUpResponse(
        Map<UUID, Integer> markedUpIdsMapping
) {
}
