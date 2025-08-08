package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.constants.DataMarkup;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.Markup;
import com.fptu.sep490.personalservice.model.enumeration.MarkupType;
import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import com.fptu.sep490.personalservice.repository.MarkupRepository;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.repository.specification.MarkupSpecifications;
import com.fptu.sep490.personalservice.service.MarkupService;
import com.fptu.sep490.personalservice.viewmodel.request.MarkupCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.response.MarkUpResponse;
import com.fptu.sep490.personalservice.viewmodel.response.MarkedUpResponse;
import com.fptu.sep490.personalservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class MarkupServiceImpl implements MarkupService {
    MarkupRepository markupRepository;
    Helper helper;
    ReadingClient readingClient;
    ListeningClient listeningClient;

    @Override
    public void addMarkup(HttpServletRequest request, MarkupCreationRequest markup) {
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));
        Markup save = Markup.builder()
                .markupType(safeEnumFromOrdinal(MarkupType.values(), markup.markUpType()))
                .taskType(safeEnumFromOrdinal(TaskType.values(), markup.taskType()))
                .practiceType(safeEnumFromOrdinal(PracticeType.values(), markup.practiceType()))
                .accountId(accountId)
                .build();
        markupRepository.save(save);
    }

    @Override
    public void deleteMarkup(HttpServletRequest request, UUID taskId) {
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));

        var markup = markupRepository.findByAccountIdAndTaskId(accountId, taskId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.MARK_UP_NOT_FOUND,
                        Constants.ErrorCode.MARK_UP_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        markupRepository.delete(markup);
    }

    @Override
    public Page<MarkUpResponse> getMarkup(int page, int size, List<Integer> markupTypeList, List<Integer> taskTypeList, List<Integer> practiceTypeList, HttpServletRequest request) {
        String accessToken = helper.getAccessToken(request);
        Pageable pageable = PageRequest.of(page, size);
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));
        var spec = MarkupSpecifications.byConditions(markupTypeList, taskTypeList, practiceTypeList, accountId);
        Page<Markup> pageResult = markupRepository.findAll(spec, pageable);
        Map<UUID, String> passageMappingTitle = new HashMap<>();
        Map<UUID, String> passageExamMappingTitle = new HashMap<>();
        Map<UUID, String> listeningTaskMappingTitle = new HashMap<>();
        Map<UUID, String> listeningExamMappingTitle = new HashMap<>();

        List<Markup> markupList = pageResult.getContent();
        markupList.forEach(item -> {
            if(item.getPracticeType().equals(PracticeType.TASK) && item.getTaskType().equals(TaskType.READING)) {
                passageMappingTitle.put(item.getTaskId(), null);
            } else if (item.getPracticeType().equals(PracticeType.EXAM) && item.getTaskType().equals(TaskType.READING)) {
                passageExamMappingTitle.put(item.getTaskId(), null);
            } else if (item.getPracticeType().equals(PracticeType.TASK) && item.getTaskType().equals(TaskType.LISTENING)) {
                listeningTaskMappingTitle.put(item.getTaskId(), null);
            } else {
                listeningExamMappingTitle.put(item.getTaskId(), null);
            }
        });

        // taọ async chạy 2 luồng để call lấy data
        var passageTitleFuture = fetchReadingTitlesAsync(accessToken, passageMappingTitle.keySet().stream().toList());
        var readingExamFuture = fetchReadingExamTitlesAsync(accessToken, passageExamMappingTitle.keySet().stream().toList());
        var listeningTitleFuture = fetchListeningTitlesAsync(accessToken, listeningTaskMappingTitle.keySet().stream().toList());
        var listeningExamFuture = fetchListeningExamTitlesAsync(accessToken, listeningExamMappingTitle.keySet().stream().toList());
        // đợi cả 2 luồng hoàn thành
        CompletableFuture.allOf(passageTitleFuture, readingExamFuture, listeningTitleFuture, listeningExamFuture).join();
        passageMappingTitle.putAll(passageTitleFuture.join());
        passageExamMappingTitle.putAll(readingExamFuture.join());
        listeningTaskMappingTitle.putAll(listeningTitleFuture.join());
        listeningExamMappingTitle.putAll(listeningExamFuture.join());

        List<MarkUpResponse> response = markupList.stream().sorted(Comparator.comparing(Markup::getCreatedAt).reversed())
                .map(item -> {
            if(item.getPracticeType().equals(PracticeType.TASK) && item.getTaskType().equals(TaskType.READING)) {
                return MarkUpResponse.builder()
                        .markUpId(item.getMarkUpId())
                        .taskTitle(passageMappingTitle.get(item.getTaskId()))
                        .markupType(item.getMarkupType().ordinal())
                        .taskType(item.getTaskType().ordinal())
                        .practiceType(item.getPracticeType().ordinal())
                        .build();
            } else if (item.getPracticeType().equals(PracticeType.EXAM) && item.getTaskType().equals(TaskType.READING)) {
                return MarkUpResponse.builder()
                        .markUpId(item.getMarkUpId())
                        .taskTitle(passageExamMappingTitle.get(item.getTaskId()))
                        .markupType(item.getMarkupType().ordinal())
                        .taskType(item.getTaskType().ordinal())
                        .practiceType(item.getPracticeType().ordinal())
                        .build();
            } else if(item.getPracticeType().equals(PracticeType.TASK) && item.getTaskType().equals(TaskType.LISTENING)) {
                return MarkUpResponse.builder()
                        .markUpId(item.getMarkUpId())
                        .taskTitle(listeningTaskMappingTitle.get(item.getTaskId()))
                        .markupType(item.getMarkupType().ordinal())
                        .taskType(item.getTaskType().ordinal())
                        .practiceType(item.getPracticeType().ordinal())
                        .build();
            } else {
                return MarkUpResponse.builder()
                        .markUpId(item.getMarkUpId())
                        .taskTitle(listeningExamMappingTitle.get(item.getTaskId()))
                        .markupType(item.getMarkupType().ordinal())
                        .taskType(item.getTaskType().ordinal())
                        .practiceType(item.getPracticeType().ordinal())
                        .build();
            }
        }).toList();
        return new PageImpl<>(response, pageable, pageResult.getTotalElements());
    }

    @Override
    public MarkedUpResponse getMarkedUpData(String type, HttpServletRequest request) {
        String userId = helper.getUserIdFromToken(request);

        switch (type) {
            case DataMarkup.READING_TASK:
                List<Markup> readingTaskMarkups = markupRepository.findMarkupByAccountIdAndTaskTypeAndPracticeType(
                        UUID.fromString(userId),
                        TaskType.READING.ordinal(),
                        PracticeType.TASK.ordinal()
                );
                Map<UUID, Integer> markUpMappingType = readingTaskMarkups.stream()
                        .collect(Collectors.toMap(
                                Markup::getTaskId,
                                markup -> markup.getMarkupType().ordinal()
                        ));
                return MarkedUpResponse.builder()
                        .markedUpIdsMapping(markUpMappingType)
                        .build();

        }
        return null;
    }

    @Async("markupExecutor")
    public CompletableFuture<Map<UUID, String>> fetchReadingTitlesAsync(String accessToken, List<UUID> taskId) {
        ResponseEntity<BaseResponse<List<TaskTitle>>> response = readingClient
                .getReadingTitle(taskId, "Bearer " + accessToken);
        var body = response.getBody();
        Map<UUID, String> passageMappingTitle = body.data().stream()
                .collect(Collectors.toMap(
                        TaskTitle::taskId,
                        TaskTitle::title
                ));
        return CompletableFuture.completedFuture(passageMappingTitle);
    }

    @Async("markupExecutor")
    public CompletableFuture<Map<UUID, String>> fetchReadingExamTitlesAsync(String accessToken, List<UUID> taskId) {
        ResponseEntity<BaseResponse<List<TaskTitle>>> response = readingClient
                .getExamTitle(taskId, "Bearer " + accessToken);
        var body = response.getBody();
        Map<UUID, String> passageMappingTitle = body.data().stream()
                .collect(Collectors.toMap(
                        TaskTitle::taskId,
                        TaskTitle::title
                ));
        return CompletableFuture.completedFuture(passageMappingTitle);
    }

    @Async("markupExecutor")
    public CompletableFuture<Map<UUID, String>> fetchListeningTitlesAsync(String accessToken, List<UUID> taskId) {
        ResponseEntity<BaseResponse<List<TaskTitle>>> response = listeningClient
                .getListeningTitle(taskId, "Bearer " + accessToken);
        var body = response.getBody();
        Map<UUID, String> passageMappingTitle = body.data().stream()
                .collect(Collectors.toMap(
                        TaskTitle::taskId,
                        TaskTitle::title
                ));
        return CompletableFuture.completedFuture(passageMappingTitle);
    }

    @Async("markupExecutor")
    public CompletableFuture<Map<UUID, String>> fetchListeningExamTitlesAsync(String accessToken, List<UUID> taskId) {
        ResponseEntity<BaseResponse<List<TaskTitle>>> response = listeningClient
                .getExamTitle(taskId, "Bearer " + accessToken);
        var body = response.getBody();
        Map<UUID, String> passageMappingTitle = body.data().stream()
                .collect(Collectors.toMap(
                        TaskTitle::taskId,
                        TaskTitle::title
                ));
        return CompletableFuture.completedFuture(passageMappingTitle);
    }

    private <T extends Enum<T>> T safeEnumFromOrdinal(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        return values[ordinal];
    }
}
