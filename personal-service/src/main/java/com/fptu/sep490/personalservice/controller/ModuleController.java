package com.fptu.sep490.personalservice.controller;


import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ShareModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
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
@RequestMapping("/module")
@Slf4j
public class ModuleController {
    ModuleService moduleService;


    @PostMapping("")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new module",
            description = "Create a new module with the provided details. Requires authentication.",
            requestBody = @RequestBody(
                    required = true,
                    description = "Request body to create a module",
                    content = @Content(schema = @Schema(implementation = ModuleRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Module created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<ModuleResponse>> createModule(
            @Valid @org.springframework.web.bind.annotation.RequestBody ModuleRequest moduleRequest,
            HttpServletRequest request
    ) throws Exception {
        ModuleResponse response = moduleService.createModule(moduleRequest, request);
        BaseResponse<ModuleResponse> baseResponse = BaseResponse.<ModuleResponse>builder()
                .data(response)
                .message("Vocabulary created successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    //API to get module by id
    @GetMapping("/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get module by ID",
            description = "Retrieve a module by its ID. Requires authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Module retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Module not found", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<ModuleResponse>> getModuleById(
            @PathVariable("moduleId") String moduleId,
            HttpServletRequest request
    ) throws Exception {
        ModuleResponse response = moduleService.getModuleById(moduleId, request);
        BaseResponse<ModuleResponse> baseResponse = BaseResponse.<ModuleResponse>builder()
                .data(response)
                .message("Module retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    //API to update module by id
    @PutMapping("/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update a module by ID",
            description = "Update a module by its ID. Requires authentication.",
            requestBody = @RequestBody(
                    required = true,
                    description = "Request body to update a module",
                    content = @Content(schema = @Schema(implementation = ModuleRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Module updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "404", description = "Module not found", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<ModuleResponse>> updateModuleById(
            @PathVariable("moduleId") String moduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ModuleRequest moduleRequest,
            HttpServletRequest request
    ) throws Exception {
        ModuleResponse response = moduleService.updateModule(moduleId, moduleRequest, request);
        BaseResponse<ModuleResponse> baseResponse = BaseResponse.<ModuleResponse>builder()
                .data(response)
                .message("Module updated successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }




    //get all modules
    @GetMapping("/my-flash-cards")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all modules",
            description = "Retrieve all modules. Requires authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Modules retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<List<ModuleResponse>>> getAllModules(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword)
    throws Exception {
        Page<ModuleResponse> responses = moduleService.getAllModules(request, page - 1, size, sortBy, sortDirection, keyword);

        Pagination pagination = Pagination.builder()
                .currentPage(responses.getNumber() + 1)
                .totalPages(responses.getTotalPages())
                .pageSize(responses.getSize())
                .totalItems((int) responses.getTotalElements())
                .hasNextPage(responses.hasNext())
                .hasPreviousPage(responses.hasPrevious())
                .build();
        BaseResponse<List<ModuleResponse>> baseResponse = BaseResponse.<List<ModuleResponse>>builder()
                .data(responses.getContent())
                .pagination(pagination)
                .message("Modules retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }


    //API get public modules
    @GetMapping("/flash-cards")
    @Operation(
            summary = "Get all public modules",
            description = "Retrieve all public modules. No authentication required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Public modules retrieved successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<List<ModuleResponse>>> getPublicModules(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) throws Exception {
        Page<ModuleResponse> responses = moduleService.getAllPublicModules(page - 1, size, sortBy, sortDirection, keyword, request);

        Pagination pagination = Pagination.builder()
                .currentPage(responses.getNumber() + 1)
                .totalPages(responses.getTotalPages())
                .pageSize(responses.getSize())
                .totalItems((int) responses.getTotalElements())
                .hasNextPage(responses.hasNext())
                .hasPreviousPage(responses.hasPrevious())
                .build();
        BaseResponse<List<ModuleResponse>> baseResponse = BaseResponse.<List<ModuleResponse>>builder()
                .data(responses.getContent())
                .pagination(pagination)
                .message("Public modules retrieved successfully")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete a module by ID",
            description = "Delete a module by its ID. Requires authentication.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Module deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<Void>> deleteModuleById(
            @PathVariable("moduleId") String moduleId,
            HttpServletRequest request
    ) throws Exception {
        moduleService.deleteModuleById(moduleId, request);
        BaseResponse<Void> baseResponse = BaseResponse.<Void>builder()
                .message("Module deleted successfully")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    // api to clone a module
    @PostMapping("/{moduleId}/clone")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Clone a module",
            description = "Clone an existing module by its ID. Requires authentication.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Module cloned successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "404", description = "Module not found", content = @Content(schema = @Schema(implementation = Exception.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = Exception.class)))
            }
    )
    public ResponseEntity<BaseResponse<ModuleResponse>> cloneModule(
            @PathVariable("moduleId") String moduleId,
            HttpServletRequest request
    ) throws Exception {
        ModuleResponse response = moduleService.cloneModule(moduleId, request);
        BaseResponse<ModuleResponse> baseResponse = BaseResponse.<ModuleResponse>builder()
                .data(response)
                .message("Module cloned successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }





}
