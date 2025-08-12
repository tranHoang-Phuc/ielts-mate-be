package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.DashboardService;
import com.fptu.sep490.personalservice.viewmodel.response.CreatorDefaultDashboard;
import com.fptu.sep490.personalservice.viewmodel.response.QuestionTypeStats;
import com.fptu.sep490.personalservice.viewmodel.response.QuestionTypeStatsWrong;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/creator-dashboard")
@Slf4j
public class CreatorDashboardController {
    DashboardService dashboardService;

    @GetMapping("/default")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<CreatorDefaultDashboard>> getDefaultDashboard(HttpServletRequest request) {
        CreatorDefaultDashboard data = dashboardService.getDashboard(request);
        BaseResponse<CreatorDefaultDashboard> response = BaseResponse.<CreatorDefaultDashboard>builder()
                .data(data)
                .message("Get data successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reading/question-type-stats")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<QuestionTypeStats>>> getQuestionTypeStats(
            @RequestParam("from_date")LocalDate fromDate,
            @RequestParam("to_date") LocalDate toDate,
            HttpServletRequest request) {
        List<QuestionTypeStats> data = dashboardService.getQuestionTypeStatsReading(fromDate, toDate, request);
        BaseResponse<List<QuestionTypeStats>> response = BaseResponse.<List<QuestionTypeStats>>builder()
                .data(data)
                .message("Get question type stats successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reading/question-type-stats/wrong")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<QuestionTypeStatsWrong>>> getQuestionTypeStatsWrong(
            @RequestParam("from_date") LocalDate fromDate,
            @RequestParam("to_date") LocalDate toDate,
            HttpServletRequest request) {
        List<QuestionTypeStatsWrong> data = dashboardService.getQuestionTypeStatsReadingWrong(fromDate, toDate, request);
        BaseResponse<List<QuestionTypeStatsWrong>> response = BaseResponse.<List<QuestionTypeStatsWrong>>builder()
                .data(data)
                .message("Get question type stats wrong successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/listening/question-type-stats")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<QuestionTypeStats>>> getListeningQuestionTypeStats(
            @RequestParam("from_date") LocalDate fromDate,
            @RequestParam("to_date") LocalDate toDate,
            HttpServletRequest request) {
        List<QuestionTypeStats> data = dashboardService.getQuestionTypeStatsListening(fromDate, toDate, request);
        BaseResponse<List<QuestionTypeStats>> response = BaseResponse.<List<QuestionTypeStats>>builder()
                .data(data)
                .message("Get listening question type stats successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/listening/question-type-stats/wrong")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<QuestionTypeStatsWrong>>> getListeningQuestionTypeStatsWrong(
            @RequestParam("from_date") LocalDate fromDate,
            @RequestParam("to_date") LocalDate toDate,
            HttpServletRequest request) {
        List<QuestionTypeStatsWrong> data = dashboardService.getQuestionTypeStatsListeningWrong(fromDate, toDate, request);
        BaseResponse<List<QuestionTypeStatsWrong>> response = BaseResponse.<List<QuestionTypeStatsWrong>>builder()
                .data(data)
                .message("Get listening question type stats wrong successfully")
                .build();
        return ResponseEntity.ok(response);
    }
}
