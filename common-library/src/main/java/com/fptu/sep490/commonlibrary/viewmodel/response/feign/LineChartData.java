package com.fptu.sep490.commonlibrary.viewmodel.response.feign;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.chrono.Chronology;

@Data
@Builder
public class LineChartData {
    private LocalDate date;
    Double value;
}
