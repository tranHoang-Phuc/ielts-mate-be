package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.DashboardService;
import com.fptu.sep490.personalservice.viewmodel.response.CreatorDefaultDashboard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/creator-dashboard")
@Slf4j
public class CreatorDashboardController {
    DashboardService dashboardService;

    @GetMapping("/default")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<CreatorDefaultDashboard>> getDefaultDashboard(HttpServletRequest request) {
        CreatorDefaultDashboard data = dashboardService.getDashboard(request);
        BaseResponse<CreatorDefaultDashboard> response = BaseResponse.<CreatorDefaultDashboard>builder()
                .data(data)
                .message("Get data successfully")
                .build();
        return ResponseEntity.ok(response);
    }
}
