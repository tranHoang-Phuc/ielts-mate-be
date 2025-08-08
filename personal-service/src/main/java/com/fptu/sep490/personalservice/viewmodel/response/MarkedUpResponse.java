package com.fptu.sep490.personalservice.viewmodel.response;

import java.util.List;
import java.util.UUID;

public record MarkedUpResponse(
        List<UUID> markedUpIds
) {
}
