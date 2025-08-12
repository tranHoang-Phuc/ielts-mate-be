package com.fptu.sep490.readingservice.service;

import com.fptu.sep490.readingservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.readingservice.model.ReportQuestionTypeStatsWrong;
import com.fptu.sep490.readingservice.viewmodel.response.DataStats;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionTypeStats;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DataStats getDataStats();

    List<ReportQuestionTypeStats> getQuestionTypeStats(LocalDate fromDate, LocalDate toDate);

    List<ReportQuestionTypeStatsWrong> getQuestionTypeStatsWrong(LocalDate fromDate, LocalDate toDate);
}
