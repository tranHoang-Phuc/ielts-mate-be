package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.readingservice.model.ReportQuestionTypeStatsWrong;
import com.fptu.sep490.readingservice.service.DashboardService;
import com.fptu.sep490.readingservice.viewmodel.response.DataStats;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionTypeStats;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard/internal")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DashboardController {
    DashboardService dashboardService;

    @GetMapping("/get-quantity")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<DataStats>> getDefault() {
        DataStats dataStats = dashboardService.getDataStats();
        BaseResponse<DataStats> response = BaseResponse.<DataStats>builder()
                .data(dataStats)
                .message("Success")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/question-type-stats")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<ReportQuestionTypeStats>>> getQuestionTypeStats(@RequestParam("fromDate")LocalDate fromDate,
                                                                                            @RequestParam("toDate") LocalDate toDate) {
        List<ReportQuestionTypeStats> questionTypeStats = dashboardService.getQuestionTypeStats(fromDate, toDate);
        BaseResponse<List<ReportQuestionTypeStats>> response = BaseResponse.<List<ReportQuestionTypeStats>>builder()
                .data(questionTypeStats)
                .message("Get question type stats successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/question-type-stats/wrong")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<ReportQuestionTypeStatsWrong>>> getQuestionTypeStatsWrong(@RequestParam("fromDate") LocalDate fromDate,
                                                                                                      @RequestParam("toDate")LocalDate toDate) {
        List<ReportQuestionTypeStatsWrong> questionTypeStatsWrong = dashboardService.getQuestionTypeStatsWrong(fromDate, toDate);
        BaseResponse<List<ReportQuestionTypeStatsWrong>> response = BaseResponse.<List<ReportQuestionTypeStatsWrong>>builder()
                .data(questionTypeStatsWrong)
                .message("Get question type stats wrong successfully")
                .build();
        return ResponseEntity.ok(response);
    }

}