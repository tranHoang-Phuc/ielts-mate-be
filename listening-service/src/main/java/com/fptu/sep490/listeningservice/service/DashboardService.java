package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStatsWrong;
import com.fptu.sep490.listeningservice.viewmodel.response.DataStats;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DataStats getDataStats();

    List<ReportQuestionTypeStats> getQuestionTypeStats(LocalDate fromDate, LocalDate toDate, HttpServletRequest request);

    List<ReportQuestionTypeStatsWrong> getQuestionTypeStatsWrong(LocalDate fromDate, LocalDate toDate, HttpServletRequest request);
}
