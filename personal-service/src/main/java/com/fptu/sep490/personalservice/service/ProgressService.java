package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.personalservice.viewmodel.response.BandLineChartResponse;
import com.fptu.sep490.personalservice.viewmodel.response.OverviewProgressResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface ProgressService {

    OverviewProgressResponse getOverviewProgress(
            String timeFrame,
            HttpServletRequest request
    );

    List<BandLineChartResponse> getBandChart(String timeFrame, LocalDate startDate, LocalDate endDate, HttpServletRequest request);
}
