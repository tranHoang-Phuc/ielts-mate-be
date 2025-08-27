package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskGetAllResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskGetResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.TaskTitle;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/listens")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ListeningTaskController {
    ListeningTaskService listeningTaskService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
           summary = "Create a new listening task",
              description = "Create a new listening task with the provided details, including audio file and transcription."
    )
    public ResponseEntity<BaseResponse<ListeningTaskResponse>> createListeningTask(
            @RequestParam("ielts_type") Integer ieltsType,
            @RequestParam("part_number") Integer partNumber,
            @RequestParam("instruction") String instruction,
            @RequestParam("title") String title,
            @RequestParam("status") Integer status,
            @RequestPart("audio_file") MultipartFile audioFile,
            @RequestParam("is_automatic_transcription") boolean isAutomaticTranscription,
            @RequestParam(value = "transcript", required = false) String transcription,
            HttpServletRequest httpServletRequest) throws IOException {
        ListeningTaskResponse response = listeningTaskService.createListeningTask(ListeningTaskCreationRequest.builder()
                        .ieltsType(ieltsType)
                        .partNumber(partNumber)
                        .instruction(instruction)
                        .status(status)
                        .title(title)
                        .audioFile(audioFile)
                        .isAutomaticTranscription(isAutomaticTranscription)
                        .transcription(transcription)
                .build(), httpServletRequest);
        BaseResponse<ListeningTaskResponse> baseResponse = BaseResponse.<ListeningTaskResponse>builder()
                .data(response)
                .message("Listening task created successfully")
                .build();
        return new ResponseEntity<>(baseResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{task-Id}")
    @Operation(
            summary = "Get listening task by ID",
            description = "Retrieve a specific listening task by its ID."
    )
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<ListeningTaskGetAllResponse>> getTaskById(@PathVariable("task-Id") UUID taskId) {
        ListeningTaskGetAllResponse data = listeningTaskService.getTaskById(taskId);
        BaseResponse<ListeningTaskGetAllResponse> response = BaseResponse.<ListeningTaskGetAllResponse>builder()
                .message("Retrieve data successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{task-id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update an existing listening task",
            description = "Update the details of an existing listening task, including audio file and transcription."
    )
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<ListeningTaskResponse>> updateListeningTask(
            @PathVariable("task-id") UUID taskId,
            @RequestParam(value = "ielts_type", required = false) Integer ieltsType,
            @RequestParam(value = "part_number", required = false) Integer partNumber,
            @RequestParam(value = "instruction", required = false) String instruction,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "title", required = false) String title,
            @RequestPart(value = "audio_file", required = false) MultipartFile audioFile,
            @RequestParam(value = "transcript", required = false) String transcription,
            HttpServletRequest httpServletRequest) throws IOException {
        ListeningTaskResponse response = listeningTaskService.updateTask(taskId,status, ieltsType, partNumber, instruction,
                title, audioFile, transcription, httpServletRequest);
        BaseResponse<ListeningTaskResponse> baseResponse = BaseResponse.<ListeningTaskResponse>builder()
                .data(response)
                .message("Listening task updated successfully")
                .build();
        return ResponseEntity.ok(baseResponse);
    }

    @DeleteMapping("/{task-id}")
    @Operation(
            summary = "Delete a listening task",
            description = "Delete a specific listening task by its ID."
    )
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<?>> deleteTask(@PathVariable("task-id") UUID taskId) {
        listeningTaskService.deleteTask(taskId);
        BaseResponse<?> baseResponse = BaseResponse.builder()
                .message("Deleted successfully")
                .build();
        return org.springframework.http.ResponseEntity.ok(baseResponse);
    }

    @GetMapping
    @Operation(
            summary = "Get activated listening tasks",
            description = "Retrieve a paginated list of activated listening tasks with optional filters."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<ListeningTaskGetResponse>>> getActivatedTask(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "ieltsType", required = false) String ieltsType,
            @RequestParam(value = "partNumber", required = false) String partNumber,
            @RequestParam(value = "questionCategory", required = false) String questionCategory,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            HttpServletRequest httpServletRequest
    ) {
        List<Integer> ieltsTypeList = parseCommaSeparatedIntegers(ieltsType);
        List<Integer> partNumberList = parseCommaSeparatedIntegers(partNumber);

        Page<ListeningTaskGetResponse> pageableTask = listeningTaskService.getActivatedTask(page - 1, size, ieltsTypeList,
                partNumberList, questionCategory, sortBy, sortDirection, title, createdBy, httpServletRequest);
        Pagination pagination = Pagination.builder()
                .currentPage(pageableTask.getNumber() + 1)
                .totalPages(pageableTask.getTotalPages())
                .pageSize(pageableTask.getSize())
                .totalItems((int) pageableTask.getTotalElements())
                .hasNextPage(pageableTask.hasNext())
                .hasPreviousPage(pageableTask.hasPrevious())
                .build();
        BaseResponse<List<ListeningTaskGetResponse>> body = BaseResponse.<List<ListeningTaskGetResponse>>builder()
                .data(pageableTask.getContent())
                .pagination(pagination)
                .message(null)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @GetMapping("/creator")
    @Operation(
            summary = "Get listening tasks for creators",
                description = "Retrieve a paginated list of listening tasks created by the user with optional filters."
    )
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<ListeningTaskGetResponse>>> getListeningTask(
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "ieltsType", required = false) String ieltsType,
            @RequestParam(value = "partNumber", required = false) String partNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "questionCategory", required = false) String questionCategory,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "createdBy", required = false) String createdBy
    ) {
        List<Integer> ieltsTypeList = parseCommaSeparatedIntegers(ieltsType);
        List<Integer> statusList = parseCommaSeparatedIntegers(status);
        List<Integer> partNumberList = parseCommaSeparatedIntegers(partNumber);

        Page<ListeningTaskGetResponse> pageableTask = listeningTaskService.getListeningTask(page - 1, size, statusList, ieltsTypeList,
                partNumberList, questionCategory, sortBy, sortDirection, title, createdBy);
        Pagination pagination = Pagination.builder()
                .currentPage(pageableTask.getNumber() + 1)
                .totalPages(pageableTask.getTotalPages())
                .pageSize(pageableTask.getSize())
                .totalItems((int) pageableTask.getTotalElements())
                .hasNextPage(pageableTask.hasNext())
                .hasPreviousPage(pageableTask.hasPrevious())
                .build();
        BaseResponse<List<ListeningTaskGetResponse>> body = BaseResponse.<List<ListeningTaskGetResponse>>builder()
                .data(pageableTask.getContent())
                .pagination(pagination)
                .message(null)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    @GetMapping("/internal/task")
    @Operation(
            summary = "Get task titles by IDs",
            description = "Retrieve a list of task titles based on the provided task IDs."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<TaskTitle>>> getTaskTitle(
            @RequestParam("task-ids") List<UUID> taskIds
    ) {
        List<TaskTitle> data = listeningTaskService.getTaskTitle(taskIds);
        BaseResponse<List<TaskTitle>> response = BaseResponse.<List<TaskTitle>>builder()
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    private List<Integer> parseCommaSeparatedIntegers(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
