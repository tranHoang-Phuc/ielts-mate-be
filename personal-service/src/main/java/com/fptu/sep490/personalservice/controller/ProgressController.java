package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/progress")
@Slf4j
public class ProgressController {

    ReadingClient readingClient;
    Helper helper;

    @GetMapping("/reading")
    public ResponseEntity<BaseResponse<OverviewProgress>> getReadingProgress(HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);
        OverviewProgressReq overviewProgressReq = new OverviewProgressReq();
        overviewProgressReq.setTimeFrame("1w");

        ResponseEntity<BaseResponse<OverviewProgress>> overviewProgress = readingClient.getExamOverview(overviewProgressReq, "Bearer " + accessToken);
        BaseResponse<OverviewProgress> response = BaseResponse.<OverviewProgress>builder()
                .data(overviewProgress.getBody().data())
                .message("Reading progress retrieved successfully")
                .build();
        return ResponseEntity.ok(response);
    }

}
