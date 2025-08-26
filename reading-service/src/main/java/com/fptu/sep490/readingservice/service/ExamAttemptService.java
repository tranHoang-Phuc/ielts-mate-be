package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;

import java.util.List;

public interface ExamAttemptService {
    SubmittedAttemptResponse submittedExam(String attemptId, ExamAttemptAnswersRequest answers, HttpServletRequest request) throws JsonProcessingException;

    CreateExamAttemptResponse createExamAttempt (String urlSlug, HttpServletRequest request) throws JsonProcessingException;

    Page<UserGetHistoryExamAttemptResponse> getListExamHistory(
            int page,
            int size,
            String readingExamName,
            String sortBy,
            String sortDirection,
            HttpServletRequest request
    );

    ExamAttemptGetDetail getExamAttemptById(String examAttemptId, HttpServletRequest request) throws JsonProcessingException;

    OverviewProgress getOverViewProgress(OverviewProgressReq body, String token);

    List<LineChartData> getBandChart(LineChartReq body, String token);

    List<ExamAttemptAI> getAttemptResultHistory(HttpServletRequest request);

    List<AIResultData> getAIResultData(HttpServletRequest request);
}
