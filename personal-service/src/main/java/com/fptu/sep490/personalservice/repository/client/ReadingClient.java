package com.fptu.sep490.personalservice.repository.client;

import com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.personalservice.viewmodel.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "reading-client", url = "${service.reading-service}")
public interface ReadingClient {
    @GetMapping(value = "/passages/internal/task", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<TaskTitle>>> getReadingTitle(@RequestParam("ids") List<UUID> ids,
                                                                  @RequestHeader("Authorization") String token);

    @GetMapping(value = "/reading-exams/internal/exam", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<TaskTitle>>> getExamTitle(@RequestParam("ids") List<UUID> ids,
                                                               @RequestHeader("Authorization") String token);

    @PostMapping(value = "/exam/attempts/internal/overview-progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<OverviewProgress>> getExamOverview(@RequestBody OverviewProgressReq overviewProgressReq,
                                                                   @RequestHeader("Authorization") String token);
    @GetMapping(value = "/dashboard/internal/get-quantity", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<DataStats>> getReadingStats(@RequestHeader("Authorization") String token);

    @GetMapping(value = "/exam/attempts/internal/suggest-ai", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<AIResultData>>> getAIData(@RequestHeader("Authorization") String token);

    @PostMapping(value = "/exam/attempts/internal/band-chart", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<LineChartData>>> getBandChart(@RequestBody LineChartReq lineChartReq,
                                                                      @RequestHeader("Authorization") String token);

    @GetMapping(value = "/dashboard/internal/question-type-stats", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<ReportQuestionTypeStats>>> getQuestionTypeStatsReading(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader("Authorization") String accessToken);

    @GetMapping(value = "/dashboard/internal/question-type-stats/wrong", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<ReportQuestionTypeStatsWrong>>> getQuestionTypeStatsReadingWrong(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader("Authorization") String accessToken);

    @GetMapping(value = "/exam/attempts/internal/band-score", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<BandScoreData>> getBandScore(@RequestHeader("Authorization") String accessToken);
}
