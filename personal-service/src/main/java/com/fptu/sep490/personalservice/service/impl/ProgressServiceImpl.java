package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.utils.DateTimeUtils;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.ProgressService;
import com.fptu.sep490.personalservice.viewmodel.response.OverviewProgressResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ProgressServiceImpl implements ProgressService {
    ReadingClient readingClient;
    ListeningClient listeningClient;
    Helper helper;

    @Async("progressExecutor")
    public CompletableFuture<OverviewProgress> fetchReadingOverviewProgress(String accessToken, OverviewProgressReq overviewProgressReq) {
        ResponseEntity<BaseResponse<OverviewProgress>> response = readingClient.getExamOverview(overviewProgressReq, "Bearer " + accessToken);
        OverviewProgress body = response.getBody().data();
        return CompletableFuture.completedFuture(body);
    }

    @Async("progressExecutor")
    public CompletableFuture<OverviewProgress> fetchListeningOverviewProgress(String accessToken, OverviewProgressReq overviewProgressReq) {
        ResponseEntity<BaseResponse<OverviewProgress>> response = listeningClient.getExamOverview(overviewProgressReq, "Bearer " + accessToken);
        OverviewProgress body = response.getBody().data();
        return CompletableFuture.completedFuture(body);
    }

    @Override
    public OverviewProgressResponse getOverviewProgress(
            OverviewProgressReq overviewProgressReq,
            HttpServletRequest request
    ) {

        String accessToken = helper.getAccessToken(request);

        CompletableFuture<OverviewProgress> readingOverview = fetchReadingOverviewProgress(accessToken, overviewProgressReq);
        CompletableFuture<OverviewProgress> listeningOverview = fetchListeningOverviewProgress(accessToken, overviewProgressReq);

        CompletableFuture.allOf(readingOverview, listeningOverview).join();
        OverviewProgress r = readingOverview.join();
        OverviewProgress l = listeningOverview.join();

        OverviewProgressResponse.ProgressOverview reading = OverviewProgressResponse.ProgressOverview.builder()
                .exam(r.getExam())
                .task(r.getTask())
                .totalExams(r.getTotalExams())
                .totalTasks(r.getTotalTasks())
                .build();
        OverviewProgressResponse.ProgressOverview listening = OverviewProgressResponse.ProgressOverview.builder()
                .exam(l.getExam())
                .task(l.getTask())
                .totalExams(l.getTotalExams())
                .totalTasks(l.getTotalTasks())
                .build();

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = DateTimeUtils.calculateStartDateFromTimeFrame(overviewProgressReq.getTimeFrame());

        Double readingBand = r.getAverageBandInTimeFrame();   // Có thể null
        Double listeningBand = l.getAverageBandInTimeFrame(); // Có thể null
        double sum = 0.0;
        int count = 0;
        if (readingBand != null) {
            sum += readingBand;
            count++;
        }
        if (listeningBand != null) {
            sum += listeningBand;
            count++;
        }
        Double averageOverallBand = (count > 0) ? sum / count : null;
        OverviewProgressResponse.BandStats bandStats = OverviewProgressResponse.BandStats.builder()
                .startDate(startDate)
                .endDate(endDate)
                .timeFrame(overviewProgressReq.getTimeFrame())
                .averageReadingBand(readingBand)
                .averageListeningBand(listeningBand)
                .averageOverallBand(averageOverallBand)
                .numberOfReadingExams(r.getNumberOfExamsInTimeFrame())
                .numberOfListeningExams(l.getNumberOfExamsInTimeFrame())
                .build();

        LocalDateTime lastLearningDate = Optional.ofNullable(r.getLastLearningDate())
                .map(rDate -> {
                    LocalDateTime lDate = l.getLastLearningDate();
                    if (lDate == null) return rDate;
                    return rDate.isAfter(lDate) ? rDate : lDate;
                })
                .orElse(l.getLastLearningDate());

        OverviewProgressResponse response = OverviewProgressResponse.builder()
                .reading(reading)
                .listening(listening)
                .bandStats(bandStats)
                .lastLearningDate(lastLearningDate)
                .build();

        return response;
    }
}
