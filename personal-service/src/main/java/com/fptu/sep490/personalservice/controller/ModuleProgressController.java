package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleFlashCardRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleProgressRequest;
import com.fptu.sep490.personalservice.viewmodel.request.FlashcardProgressRequest;
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

    @PutMapping("/progress/{moduleId}/flashcard")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update flashcard progress",
            description = "This API updates flashcard learning progress for a specific module."
    )
    public ResponseEntity<BaseResponse<String>> updateFlashcardProgress(
            @PathVariable("moduleId") String moduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody FlashcardProgressRequest request,
            HttpServletRequest httpRequest
    ) throws Exception {
        moduleService.updateFlashcardProgress(moduleId, request, httpRequest);
        BaseResponse<String> baseResponse = BaseResponse.<String>builder()
                .data("Progress updated successfully")
                .message("Flashcard progress updated successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    // API to refresh module progress of user
    @PutMapping("/refresh-progress/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Refresh module progress of user or Complete module",
            description = "This API refreshes the module progress of the authenticated user."
    )
    public ResponseEntity<BaseResponse<ModuleProgressResponse>> refreshModuleProgress(
            @PathVariable("moduleId") String moduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ModuleFlashCardRequest moduleFlashCardRequest,
            HttpServletRequest request
    ) throws Exception {
        ModuleProgressResponse response = moduleService.refreshModuleProgress(moduleId, moduleFlashCardRequest, request);
        BaseResponse<ModuleProgressResponse> baseResponse = BaseResponse.<ModuleProgressResponse>builder()
                .data(response)
                .message("Module progress refreshed successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    // API to get overall module progress of user
    @GetMapping("/overall-progress/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get overall module progress of user",
            description = "This API retrieves the overall module progress of the authenticated user."
    )
    public ResponseEntity<BaseResponse<ModuleProgressResponse>> getOverallModuleProgress(
            @PathVariable("moduleId") String moduleId,
            HttpServletRequest request
    ) throws Exception {
        ModuleProgressResponse response = moduleService.getModuleProgress(moduleId, request);
        BaseResponse<ModuleProgressResponse> baseResponse = BaseResponse.<ModuleProgressResponse>builder()
                .data(response)
                .message("Overall module progress retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

}
