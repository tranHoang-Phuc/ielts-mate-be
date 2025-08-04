package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.service.ProgressService;
import com.fptu.sep490.personalservice.viewmodel.response.OverviewProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    ProgressService progressService;

    @GetMapping("/overview")
    @Operation(
            summary = "Get overview progress of reading and listening exams",
            description = "This endpoint retrieves the overview progress of reading and listening exams for the authenticated user."
    )
    public ResponseEntity<BaseResponse<OverviewProgressResponse>> getReadingProgress(HttpServletRequest request, OverviewProgressReq overviewProgressReq) {

        OverviewProgressResponse overviewProgress = progressService.getOverviewProgress(overviewProgressReq, request);
        return ResponseEntity.ok(
                BaseResponse.<OverviewProgressResponse>builder()
                        .message(null)
                        .data(overviewProgress)
                        .build()
        );
    }

}
