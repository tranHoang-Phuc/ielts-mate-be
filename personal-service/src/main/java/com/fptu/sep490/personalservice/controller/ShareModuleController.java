package com.fptu.sep490.personalservice.controller;



import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.personalservice.model.enumeration.ModuleUserStatus;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ShareModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleUserResponse;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/module-share")
@Slf4j
public class ShareModuleController {

    ModuleService moduleService;

    //API to share a module to other user, chỉ cần truyền list user_id ở body
    @PostMapping("/{moduleId}/share")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Share a module with other users",
            description = "Share a module with other users by providing their user IDs. Requires authentication.",
            requestBody = @RequestBody(
                    required = true,
                    description = "Request body to share a module",
                    content = @Content(schema = @Schema(implementation = ShareModuleRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Module shared successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "404", description = "Module not found", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<Void>> shareModule(
            @PathVariable("moduleId") String moduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ShareModuleRequest moduleRequest,
            HttpServletRequest request
    ) throws Exception {
        moduleService.shareModule(moduleId, moduleRequest, request);
        BaseResponse<Void> baseResponse = BaseResponse.<Void>builder()
                .message("Module shared successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    //API to get all shared modules of user
    @GetMapping("/my-shared")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all shared modules user accepted",
            description = "Retrieve all modules shared with the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Shared modules retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<List<ModuleUserResponse>>> getAllSharedModules(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword
    ) throws Exception {
        Page<ModuleUserResponse> responses = moduleService.getAllSharedModules(request, page - 1, size, sortBy, sortDirection, keyword, ModuleUserStatus.ACCEPTED.ordinal());

        Pagination pagination = Pagination.builder()
                .currentPage(responses.getNumber() + 1)
                .totalPages(responses.getTotalPages())
                .pageSize(responses.getSize())
                .totalItems((int) responses.getTotalElements())
                .hasNextPage(responses.hasNext())
                .hasPreviousPage(responses.hasPrevious())
                .build();
        BaseResponse<List<ModuleUserResponse>> baseResponse = BaseResponse.<List<ModuleUserResponse>>builder()
                .data(responses.getContent())
                .pagination(pagination)
                .message("Shared modules retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    //API to get all shared modules of user with status 0 (pending) -> i can accept or deny the request
    @GetMapping("/my-shared/requests")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all shared module requests are pending, i can accept or deny",
            description = "Retrieve all module sharing requests for the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Shared module requests retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<List<ModuleUserResponse>>> getAllSharedModuleRequests(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword
    ) throws Exception {
        Page<ModuleUserResponse> responses = moduleService.getAllSharedModules(request, page - 1, size, sortBy, sortDirection, keyword, ModuleUserStatus.PENDING.ordinal());

        Pagination pagination = Pagination.builder()
                .currentPage(responses.getNumber() + 1)
                .totalPages(responses.getTotalPages())
                .pageSize(responses.getSize())
                .totalItems((int) responses.getTotalElements())
                .hasNextPage(responses.hasNext())
                .hasPreviousPage(responses.hasPrevious())
                .build();
        BaseResponse<List<ModuleUserResponse>> baseResponse = BaseResponse.<List<ModuleUserResponse>>builder()
                .data(responses.getContent())
                .pagination(pagination)
                .message("Shared module requests retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }
    //API to accept or deny a shared module request
    @PutMapping("/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Accept or deny a shared module request",
            description = "Accept or deny a shared module request by providing the module ID and status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Shared module request updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "404", description = "Module not found", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<Void>> updateSharedModuleRequest(
            @PathVariable("moduleId") String moduleId,
            @RequestParam("status") int status,
            HttpServletRequest request
    ) throws Exception {
        moduleService.updateSharedModuleRequest(moduleId, status, request);
        BaseResponse<Void> baseResponse = BaseResponse.<Void>builder()
                .message("Shared module request updated successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    // API to get all my shared modules
    @GetMapping("/my-shared/requested")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all modules shared by me",
            description = "Retrieve all modules that I have shared with other users.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Shared modules retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<List<ModuleUserResponse>>> getAllMySharedModules(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword
    ) throws Exception {
        Page<ModuleUserResponse> responses = moduleService.getAllMySharedModules(request, page - 1, size, sortBy, sortDirection, keyword);

        Pagination pagination = Pagination.builder()
                .currentPage(responses.getNumber() + 1)
                .totalPages(responses.getTotalPages())
                .pageSize(responses.getSize())
                .totalItems((int) responses.getTotalElements())
                .hasNextPage(responses.hasNext())
                .hasPreviousPage(responses.hasPrevious())
                .build();
        BaseResponse<List<ModuleUserResponse>> baseResponse = BaseResponse.<List<ModuleUserResponse>>builder()
                .data(responses.getContent())
                .pagination(pagination)
                .message("My shared modules retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }


}
