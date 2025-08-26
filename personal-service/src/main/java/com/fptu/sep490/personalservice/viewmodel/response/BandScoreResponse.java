package com.fptu.sep490.personalservice.viewmodel.response;

import lombok.Builder;

@Builder
public record BandScoreResponse(
        Double listening,
        Double reading
) {
}
