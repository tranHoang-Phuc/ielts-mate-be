package com.fptu.sep490.commonlibrary.viewmodel.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LineChartReq {
    private String timeFrame;
    private LocalDate startDate;
    private LocalDate endDate;
}
