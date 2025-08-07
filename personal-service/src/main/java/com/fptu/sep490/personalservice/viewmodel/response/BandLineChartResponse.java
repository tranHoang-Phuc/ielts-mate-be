package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BandLineChartResponse {
    @Builder.Default
    List<LineChartData> readingData = new ArrayList<>();

    @Builder.Default
    List<LineChartData> listeningData = new ArrayList<>();
}
