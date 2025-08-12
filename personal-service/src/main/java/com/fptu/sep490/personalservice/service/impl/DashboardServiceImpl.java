package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.DashboardService;
import com.fptu.sep490.personalservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    ReadingClient readingClient;
    ListeningClient listeningClient;
    Helper helper;

    @Override
    public CreatorDefaultDashboard getDashboard(HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);

        var readingStats = fetchReadingStats(accessToken);
        var listeningStats = fetchListeningStats(accessToken);
        CompletableFuture.allOf(readingStats, listeningStats);
        DataStats readingStatsData = readingStats.join();
        DataStats listeningStatsData = listeningStats.join();

        // Count attempted tasks and exams
        Set<String> usedColorsInAvgReading = new HashSet<>();
        Set<String> usedColorsInAvgListening = new HashSet<>();
        Set<String> usedColorsInQuestionTypeReading = new HashSet<>();
        Set<String> usedColorsInQuestionTypeListening = new HashSet<>();
        Set<String> usedColorsInQuestionTypeReadingWrong = new HashSet<>();
        Set<String> usedColorsInQuestionTypeListeningWrong = new HashSet<>();
        return CreatorDefaultDashboard.builder()
                .numberOfReadingTasks(readingStatsData.numberOfTasks())
                .numberOfListeningTasks(listeningStatsData.numberOfTasks())
                .numberOfReadingExams(readingStatsData.numberOfExams())
                .numberOfListeningExams(listeningStatsData.numberOfExams())
                .userInAvgBranchScoreReading(readingStatsData.userInBranchAvg().stream()
                        .map(p -> UserBranchScore.builder()
                                .branchScore(p.branchScore())
                                .numberOfUsers(p.numberOfUsers())
                                // random color for each branch and color does not repeat
                                .color(helper.getRandomColor(usedColorsInAvgReading))
                                .build()).toList())
                .userInAvgBranchScoreListening(listeningStatsData.userInBranchAvg().stream()
                        .map(p -> UserBranchScore.builder()
                                .branchScore(p.branchScore())
                                .numberOfUsers(p.numberOfUsers())
                                .color(helper.getRandomColor(usedColorsInAvgListening))
                                .build()).toList())
                .questionTypeStatsReading(readingStatsData.questionTypeStats().stream()
                        .map(d -> QuestionTypeStats.builder()
                                .questionType(d.questionType())
                                .correctCount(d.correctCount())
                                .color(helper.getRandomColor(usedColorsInQuestionTypeReading))
                                .build()).toList())
                .questionTypeStatsListening(listeningStatsData.questionTypeStats().stream()
                        .map(d -> QuestionTypeStats.builder()
                                .questionType(d.questionType())
                                .correctCount(d.correctCount())
                                .color(helper.getRandomColor(usedColorsInQuestionTypeListening))
                                .build()).toList())
                .questionTypeStatsReadingWrong(readingStatsData.questionTypeStatsWrong().stream()
                        .map(d -> QuestionTypeStatsWrong.builder()
                                .wrongCount(d.wrongCount())
                                .questionType(d .questionType())
                                .color(helper.getRandomColor(usedColorsInQuestionTypeReadingWrong))
                                .build()).toList())
                .questionTypeStatsListeningWrong(listeningStatsData.questionTypeStatsWrong().stream()
                        .map(d -> QuestionTypeStatsWrong.builder()
                                .wrongCount(d.wrongCount())
                                .questionType(d.questionType())
                                .color(helper.getRandomColor(usedColorsInQuestionTypeListeningWrong))
                                .build()).toList())
                .build();
    }

    @Override
    public List<QuestionTypeStats> getQuestionTypeStatsReading(LocalDate fromDate, LocalDate toDate, HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);
        Set<String> usedColors = new HashSet<>();
        ResponseEntity<BaseResponse<List<ReportQuestionTypeStats>>> response = readingClient.getQuestionTypeStatsReading(
                fromDate, toDate,"Bearer " + accessToken);
        var body = response.getBody();
        if (body == null || !response.getStatusCode().is2xxSuccessful()) {
            return List.of();
        }
        return body.data().stream().map(
                d -> QuestionTypeStats.builder()
                        .questionType(d.questionType())
                        .correctCount(d.correctCount())
                        .color(helper.getRandomColor(usedColors))
                        .build()
        ).toList();
    }

    @Override
    public List<QuestionTypeStatsWrong> getQuestionTypeStatsReadingWrong(LocalDate fromDate, LocalDate toDate, HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);
        Set<String> usedColors = new HashSet<>();
        ResponseEntity<BaseResponse<List<ReportQuestionTypeStatsWrong>>> response = readingClient.getQuestionTypeStatsReadingWrong(
                fromDate, toDate, "Bearer " + accessToken);
        var body = response.getBody();
        if (body == null || !response.getStatusCode().is2xxSuccessful()) {
            return List.of();
        }
        return body.data().stream().map(
                d -> QuestionTypeStatsWrong.builder()
                        .questionType(d.questionType())
                        .wrongCount(d.wrongCount())
                        .color(helper.getRandomColor(usedColors))
                        .build()
        ).toList();
    }

    @Override
    public List<QuestionTypeStats> getQuestionTypeStatsListening(LocalDate fromDate, LocalDate toDate, HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);
        Set<String> usedColors = new HashSet<>();
        ResponseEntity<BaseResponse<List<ReportQuestionTypeStats>>> response = listeningClient.getQuestionTypeStatsListening(
                fromDate, toDate, "Bearer " + accessToken);
        var body = response.getBody();
        if (body == null || !response.getStatusCode().is2xxSuccessful()) {
            return List.of();
        }
        return body.data().stream().map(
                d -> QuestionTypeStats.builder()
                        .questionType(d.questionType())
                        .correctCount(d.correctCount())
                        .color(helper.getRandomColor(usedColors))
                        .build()
        ).toList();
    }

    @Override
    public List<QuestionTypeStatsWrong> getQuestionTypeStatsListeningWrong(LocalDate fromDate, LocalDate toDate, HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);
        Set<String> usedColors = new HashSet<>();
        ResponseEntity<BaseResponse<List<ReportQuestionTypeStats>>> response = listeningClient.getQuestionTypeStatsListening(
                fromDate, toDate, "Bearer " + accessToken);
        var body = response.getBody();
        if (body == null || !response.getStatusCode().is2xxSuccessful()) {
            return List.of();
        }
        return body.data().stream().map(
                d -> QuestionTypeStatsWrong.builder()
                        .questionType(d.questionType())
                        .wrongCount(d.correctCount())
                        .color(helper.getRandomColor(usedColors))
                        .build()
        ).toList();
    }

    @Async("statsExecutor")
    protected CompletableFuture<DataStats> fetchListeningStats(String accessToken) {
        ResponseEntity<BaseResponse<DataStats>> response = listeningClient.getListeningStats("Bearer " + accessToken);
        var body = response.getBody();
        if (body == null || !response.getStatusCode().is2xxSuccessful()) {
            return CompletableFuture.completedFuture(DataStats.builder()
                            .numberOfExams(0)
                            .numberOfTasks(0)
                    .build());
        }
        return CompletableFuture.completedFuture(body.data());
    }

    @Async("statsExecutor")
    protected CompletableFuture<DataStats> fetchReadingStats(String accessToken) {
        ResponseEntity<BaseResponse<DataStats>> response = readingClient.getReadingStats("Bearer " + accessToken);
        var body = response.getBody();
        if (body == null || !response.getStatusCode().is2xxSuccessful()) {
            return CompletableFuture.completedFuture(DataStats.builder()
                            .numberOfExams(0)
                            .numberOfTasks(0)
                    .build());
        }
        return CompletableFuture.completedFuture(body.data());
    }
}
