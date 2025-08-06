package com.fptu.sep490.commonlibrary.viewmodel.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LineChartReq {
    private String timeFrame;
    private LocalDate startDate;
    private LocalDate endDate;
}
