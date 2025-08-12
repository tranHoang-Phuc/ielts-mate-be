package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.response.CreatorDefaultDashboard;
import com.fptu.sep490.personalservice.viewmodel.response.QuestionTypeStats;
import com.fptu.sep490.personalservice.viewmodel.response.QuestionTypeStatsWrong;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    CreatorDefaultDashboard getDashboard(HttpServletRequest request);

    List<QuestionTypeStats> getQuestionTypeStatsReading(LocalDate fromDate, LocalDate toDate, HttpServletRequest request);

    List<QuestionTypeStatsWrong> getQuestionTypeStatsReadingWrong(LocalDate fromDate, LocalDate toDate, HttpServletRequest request);

    List<QuestionTypeStats> getQuestionTypeStatsListening(LocalDate fromDate, LocalDate toDate, HttpServletRequest request);

    List<QuestionTypeStatsWrong> getQuestionTypeStatsListeningWrong(LocalDate fromDate, LocalDate toDate, HttpServletRequest request);
}
