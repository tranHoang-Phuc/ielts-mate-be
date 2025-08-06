package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.DashboardService;
import com.fptu.sep490.listeningservice.viewmodel.response.DataStats;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard/internal")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DashboardController {
    DashboardService dashboardService;

    @GetMapping("/get-quantity")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<DataStats>> getDefault() {
        DataStats dataStats = dashboardService.getDataStats();
        BaseResponse<DataStats> response = BaseResponse.<DataStats>builder()
                .data(dataStats)
                .message("Success")
                .build();
        return ResponseEntity.ok(response);
    }

}
