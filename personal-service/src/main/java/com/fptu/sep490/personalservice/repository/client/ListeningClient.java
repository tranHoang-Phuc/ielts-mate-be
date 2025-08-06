package com.fptu.sep490.personalservice.repository.client;

import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.personalservice.viewmodel.response.DataStats;
import com.fptu.sep490.personalservice.viewmodel.response.TaskTitle;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "listening-client", url = "${service.listening-service}")
public interface ListeningClient {
    @GetMapping(value = "/listens/internal/task", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<TaskTitle>>> getListeningTitle(@RequestParam("task-ids") List<UUID> ids,
                                                                    @RequestHeader("Authorization") String token);

    @GetMapping(value = "/exams/internal/exam", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<List<TaskTitle>>> getExamTitle(@RequestParam("ids") List<UUID> ids,
                                                               @RequestHeader("Authorization") String token);

    @PostMapping(value = "/exam/attempts/internal/overview-progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<OverviewProgress>> getExamOverview(@RequestBody OverviewProgressReq overviewProgressReq,
                                                                   @RequestHeader("Authorization") String token);

    @GetMapping(value = "/dashboard/internal/get-quantity", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<DataStats>> getListeningStats(@RequestHeader("Authorization") String token);
}
