package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ExamAttempt;
import com.fptu.sep490.listeningservice.model.ListeningExam;
import com.fptu.sep490.listeningservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.listeningservice.model.specification.ExamAttemptSpecification;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.ExamAttemptService;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamAttemptGetDetail;
import com.fptu.sep490.listeningservice.viewmodel.response.UserGetHistoryExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ExamAttemptServiceImpl implements ExamAttemptService {
    ListeningExamRepository listeningExamRepository;
    QuestionRepository questionRepository;
    ExamAttemptRepository examAttemptRepository;
    ObjectMapper objectMapper;
    ChoiceRepository choiceRepository;
    DragItemRepository dragItemRepository;
    Helper helper;
    ListeningTaskService listeningTaskService;

    @Transactional
    @Override
    public CreateExamAttemptResponse createExamAttempt(String urlSlug, HttpServletRequest request){
        ListeningExam originalExam = listeningExamRepository
                .findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(urlSlug)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.LISTENING_EXAM_NOT_FOUND,
                        Constants.ErrorCode.LISTENING_EXAM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        ListeningExam currentExam = listeningExamRepository.findCurrentChildByParentId(originalExam.getListeningExamId())
                .orElse(originalExam);

        String userId = helper.getUserIdFromToken(request);
        UserInformationResponse user = helper.getUserInformationResponse(userId);

        //create examAttempt
        ExamAttempt examAttempt = ExamAttempt.builder()
                .duration(null) // Initialize duration to null, will be updated later
                .totalPoint(null) // Initialize totalPoint to null, will be updated later
                .listeningExam(currentExam)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        //save examAttempt
        examAttempt = examAttemptRepository.saveAndFlush(examAttempt);

        CreateExamAttemptResponse.ListeningExamResponse listeningExamResponse = CreateExamAttemptResponse.ListeningExamResponse.builder()
                .listeningExamId(currentExam.getListeningExamId())
                .listeningExamName(currentExam.getExamName())
                .listeningExamDescription(currentExam.getExamDescription())
                .urlSlug(currentExam.getUrlSlug())
                .listeningTaskIdPart1(listeningTaskService.fromListeningTask(currentExam.getPart1().getTaskId().toString()))
                .listeningTaskIdPart2(listeningTaskService.fromListeningTask(currentExam.getPart2().getTaskId().toString()))
                .listeningTaskIdPart3(listeningTaskService.fromListeningTask(currentExam.getPart3().getTaskId().toString()))
                .build();
        return CreateExamAttemptResponse.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .urlSlug(currentExam.getUrlSlug())
                .createdBy(user)
                .createdAt(examAttempt.getCreatedAt().toString())
                .listeningExam(listeningExamResponse)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public ExamAttemptGetDetail getExamAttemptById(String examAttemptId, HttpServletRequest request) throws JsonProcessingException {
        UUID attemptId = UUID.fromString(examAttemptId);
        ExamAttempt examAttempt = examAttemptRepository.findById(attemptId).orElseThrow(
                () -> new AppException(
                        Constants.ErrorCodeMessage.EXAM_ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                )
        );

        String userId = helper.getUserIdFromToken(request);
        if (!examAttempt.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.EXAM_ATTEMPT_NOT_FOUND,
                    Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        ExamAttemptHistory history = objectMapper.readValue(examAttempt.getHistory(), ExamAttemptHistory.class);

        List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse> taskResponses = listeningTaskService.fromExamAttemptHistory(history);
        taskResponses = taskResponses.stream()
                .sorted(Comparator.comparing(ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse::partNumber))
                .toList();
        ExamAttemptGetDetail.ListeningExamResponse readingExamResponse = ExamAttemptGetDetail.ListeningExamResponse.builder()
                .listeningExamId(examAttempt.getListeningExam().getListeningExamId())
                .listeningExamName(examAttempt.getListeningExam().getExamName())
                .listeningExamDescription(examAttempt.getListeningExam().getExamDescription())
                .urlSlug(examAttempt.getListeningExam().getUrlSlug())
                .listeningTaskIdPart1(taskResponses.get(0))
                .listeningTaskIdPart2(taskResponses.get(1))
                .listeningTaskIdPart3(taskResponses.get(2))
                .build();
        return ExamAttemptGetDetail.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .readingExam(readingExamResponse)
                .duration(examAttempt.getDuration().longValue())
                .totalQuestion(examAttempt.getTotalPoint())
                .createdBy(helper.getUserInformationResponse(examAttempt.getCreatedBy()))
                .updatedBy(helper.getUserInformationResponse(examAttempt.getUpdatedBy()))
                .createdAt(examAttempt.getCreatedAt().toString())
                .updatedAt(examAttempt.getUpdatedAt().toString())
                .answers(history.getUserAnswers())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserGetHistoryExamAttemptResponse> getListExamHistory(
            int page,
            int size,
            String listeningExamName,
            String sortBy,
            String sortDirection,
            HttpServletRequest request
    ) {

        Pageable pageable = PageRequest.of(page, size);
        var spec = ExamAttemptSpecification.byConditions(
                listeningExamName,
                sortBy,
                sortDirection,
                helper.getUserIdFromToken(request)
        );

        Page<ExamAttempt> examAttemptsResult = examAttemptRepository.findAll(spec, pageable);

        List<ExamAttempt> examAttempts = examAttemptsResult.getContent();

        List<UserGetHistoryExamAttemptResponse> list = examAttempts.stream().map(examAttempt -> {
            UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptListeningExamResponse listeningExamResponse =
                    UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptListeningExamResponse.builder()
                            .listeningExamId(examAttempt.getListeningExam().getListeningExamId())
                            .listeningExamName(examAttempt.getListeningExam().getExamName())
                            .listeningExamDescription(examAttempt.getListeningExam().getExamDescription())
                            .urlSlug(examAttempt.getListeningExam().getUrlSlug())
                            .build();

            return UserGetHistoryExamAttemptResponse.builder()
                    .examAttemptId(examAttempt.getExamAttemptId())
                    .listeningExam(listeningExamResponse)
                    .duration(examAttempt.getDuration())
                    .totalQuestion(examAttempt.getTotalPoint())
                    .createdBy(helper.getUserInformationResponse(examAttempt.getCreatedBy()))
                    .updatedBy(helper.getUserInformationResponse(examAttempt.getUpdatedBy()))
                    .createdAt(examAttempt.getCreatedAt().toString())
                    .updatedAt(examAttempt.getUpdatedAt().toString())
                    .build();
        }).toList();

        return new PageImpl<>(list, pageable, examAttemptsResult.getTotalElements());

    }
}
