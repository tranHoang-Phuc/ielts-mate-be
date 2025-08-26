package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.ProgressService;
import com.fptu.sep490.personalservice.viewmodel.response.BandLineChartResponse;
import com.fptu.sep490.personalservice.viewmodel.response.BandScoreResponse;
import com.fptu.sep490.personalservice.viewmodel.response.OverviewProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/progress")
@Slf4j
public class ProgressController {

    ProgressService progressService;

    @GetMapping("/overview")
    @Operation(
            summary = "Get overview progress of reading and listening exams of the authenticated user",
            description = "RequestBody: timeFrame: 1d, 1w, 1m, 1y, ... Nếu truyền sai format -> default 1w"
    )
    public ResponseEntity<BaseResponse<OverviewProgressResponse>> getReadingProgress(HttpServletRequest request, @RequestParam(value = "timeFrame", required = false, defaultValue = "1w") String timeFrame){

        OverviewProgressResponse overviewProgress = progressService.getOverviewProgress(timeFrame, request);
        return ResponseEntity.ok(
                BaseResponse.<OverviewProgressResponse>builder()
                        .message(null)
                        .data(overviewProgress)
                        .build()
        );
    }

    @GetMapping("/band-chart")
    @Operation(
            summary = "Get band chart data for reading and listening exams",
            description = "Fetches band chart data based on the provided time frame."
    )
    public ResponseEntity<BaseResponse<BandLineChartResponse>> getBandChartData(
            HttpServletRequest request,
            @Parameter(
                    description = "Time frame for the band chart data. Default is 1 day (1d). Group các nhóm theo timeFrame: 1d, 3d, 1w, 1m, 1y",
                    example = "1d"
            )
            @RequestParam(value = "timeFrame",  defaultValue = "1d") String timeFrame,
            @Parameter(
                    description = "Start date for the band chart data. Format: yyyy-MM-dd. Nếu ko truyền vào thì lấy band từ exam đầu tiên",
                    example = "2023-01-01"
            )
            @RequestParam(value = "startDate",  required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(
                    description = "End date for the band chart data. Format: yyyy-MM-dd. Nếu ko truyền vào thì lấy band đến exam cuối",
                    example = "2023-12-31"
            )
            @RequestParam(value = "endDate",    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            ) {

        BandLineChartResponse bandChartData = progressService.getBandChart(timeFrame, startDate, endDate, request);

        return ResponseEntity.ok(
                BaseResponse.<BandLineChartResponse>builder()
                        .message(null)
                        .data(bandChartData)
                        .build()
        );
    }

    @GetMapping("/band-score")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<BandScoreResponse>> getBandScore(HttpServletRequest request) {
        BandScoreResponse bandScore = progressService.getBandScore(request);
        BaseResponse<BandScoreResponse> response = BaseResponse.<BandScoreResponse>builder()
                .data(bandScore)
                .message(null)
                .build();
        return ResponseEntity.ok(response);
    }

}
