package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleProgressRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/module-share")
@Slf4j
public class ModuleProgressController {
    ModuleService moduleService;

    // api to get all module progress of user
    @GetMapping("/progress/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get module progress of user",
            description = "This API retrieves all module progress of the authenticated user."
    )
    public ResponseEntity<BaseResponse<ModuleProgressResponse>> getModuleProgress(
            @PathVariable("moduleId") String moduleId,
            HttpServletRequest request
    ) throws Exception {
        ModuleProgressResponse response = moduleService.getModuleProgress(moduleId, request);
        BaseResponse<ModuleProgressResponse> baseResponse = BaseResponse.<ModuleProgressResponse>builder()
                .data(response)
                .message("Module progress retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @PutMapping("/progress/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update module progress of user",
            description = "This API updates the module progress of the authenticated user."
    )
    public ResponseEntity<BaseResponse<ModuleProgressResponse>> updateModuleProgress(
            @PathVariable("moduleId") String moduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ModuleProgressRequest moduleProgressResponse,
            HttpServletRequest request
    ) throws Exception {
        // Assuming there's a method in ModuleService to update progress
        ModuleProgressResponse response = moduleService.updateModuleProgress(moduleId, moduleProgressResponse, request);
        BaseResponse<ModuleProgressResponse> baseResponse = BaseResponse.<ModuleProgressResponse>builder()
                .data(response)
                .message("Module progress updated successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);

    }

}
