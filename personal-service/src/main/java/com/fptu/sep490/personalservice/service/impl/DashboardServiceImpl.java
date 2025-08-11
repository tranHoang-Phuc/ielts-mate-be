package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.DashboardService;
import com.fptu.sep490.personalservice.viewmodel.response.CreatorDefaultDashboard;
import com.fptu.sep490.personalservice.viewmodel.response.DataStats;
import com.fptu.sep490.personalservice.viewmodel.response.UserBranchScore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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


        return CreatorDefaultDashboard.builder()
                .numberOfReadingTasks(readingStatsData.numberOfTasks())
                .numberOfListeningTasks(listeningStatsData.numberOfTasks())
                .numberOfReadingExams(readingStatsData.numberOfExams())
                .numberOfListeningExams(listeningStatsData.numberOfExams())
                .userInAvgBranchScoreListening(readingStatsData.userInBranchAvg().stream()
                        .map(p -> UserBranchScore.builder()
                                .branchScore(p.branchScore())
                                .numberOfUsers(p.numberOfUsers())
                                .color()
                                .build()).toList())
                .build();
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
