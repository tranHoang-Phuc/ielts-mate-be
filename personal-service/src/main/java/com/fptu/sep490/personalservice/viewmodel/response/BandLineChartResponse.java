package com.fptu.sep490.personalservice.viewmodel.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BandLineChartResponse {
    private LocalDate date;
    @Schema(description = "Reading band, nullable")
    private Double readingBand;
    @Schema(description = "Listening band, nullable")
    private Double listeningBand;
}
