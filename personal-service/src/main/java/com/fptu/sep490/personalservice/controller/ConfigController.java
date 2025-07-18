package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.ConfigService;
import com.fptu.sep490.personalservice.viewmodel.response.StreakConfigResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/config")
@Slf4j
public class ConfigController {
    ConfigService configService;

    @GetMapping("/streak")
    public ResponseEntity<BaseResponse<StreakConfigResponse>> getStreak(HttpServletRequest request) {
        StreakConfigResponse data = configService.getStreak(request);
        BaseResponse<StreakConfigResponse> response = BaseResponse.<StreakConfigResponse>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reminder")
    public ResponseEntity<BaseResponse<ReminderConfigResponse>> getReminderConfig(HttpServletRequest request) {
        ReminderConfigResponse data = configService.getReminder(request);
        BaseResponse<ReminderConfigResponse> response = BaseResponse.<ReminderConfigResponse>builder()

                .build();
    }
}
