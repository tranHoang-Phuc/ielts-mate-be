package com.fptu.sep490.personalservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.model.json.TargetConfig;
import com.fptu.sep490.personalservice.service.ConfigService;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigUpdateRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ReminderConfigResponse;
import com.fptu.sep490.personalservice.viewmodel.response.StreakConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/config")
@Slf4j
public class ConfigController {
    ConfigService configService;

    @Operation(summary = "Get current streak", description = "Retrieve the user's current streak configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Streak config retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
    })
    @GetMapping("/streak")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StreakConfigResponse>> getStreak(HttpServletRequest request) throws JsonProcessingException {
        StreakConfigResponse data = configService.getStreak(request);
        BaseResponse<StreakConfigResponse> response = BaseResponse.<StreakConfigResponse>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Get reminder configuration", description = "Retrieve the user's reminder configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reminder config retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
    })
    @GetMapping("/reminder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ReminderConfigResponse>> getReminderConfig(HttpServletRequest request) {
        ReminderConfigResponse data = configService.getReminder(request);
        BaseResponse<ReminderConfigResponse> response = BaseResponse.<ReminderConfigResponse>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register reminder", description = "Create a new reminder configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reminder config created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
    })
    @PostMapping("/reminder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ReminderConfigResponse>> registerReminder(
           @RequestBody ReminderConfigCreationRequest reminderConfigCreationRequest,
            HttpServletRequest request) {
        ReminderConfigResponse data = configService.registerReminder(reminderConfigCreationRequest, request);
        BaseResponse<ReminderConfigResponse> response = BaseResponse.<ReminderConfigResponse>builder()
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update reminder", description = "Update an existing reminder configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reminder config updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
    })
    @PutMapping("/reminder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ReminderConfigResponse>> updateReminder(
            @RequestBody ReminderConfigUpdateRequest reminderConfig,
            HttpServletRequest request) {
        ReminderConfigResponse data = configService.updateReminder(reminderConfig, request);
        BaseResponse<ReminderConfigResponse> response = BaseResponse.<ReminderConfigResponse>builder()
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get target configuration", description = "Retrieve the user's target configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Target config retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
    })
    @GetMapping("target")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<TargetConfig>> getTarget(HttpServletRequest request) throws JsonProcessingException {
        TargetConfig data = configService.getTarget(request);
        BaseResponse<TargetConfig> response = BaseResponse.<TargetConfig>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create or update target", description = "Add or update the user's target configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Target config added/updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class)))
    })
    @PostMapping("target")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<TargetConfig>> addOrUpdate(HttpServletRequest request,
                                                                  @RequestBody TargetConfig targetConfig) throws JsonProcessingException {
        TargetConfig data = configService.addOrUpdate(request, targetConfig);
        BaseResponse<TargetConfig> response = BaseResponse.<TargetConfig>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
}
