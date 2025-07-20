package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ExamAttempt;
import com.fptu.sep490.listeningservice.model.ListeningExam;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.ExamAttemptService;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .readingPassageIdPart1(listeningTaskService.fromListeningTask(currentExam.getPart1().getTaskId().toString()))
                .readingPassageIdPart2(listeningTaskService.fromListeningTask(currentExam.getPart2().getTaskId().toString()))
                .readingPassageIdPart3(listeningTaskService.fromListeningTask(currentExam.getPart3().getTaskId().toString()))
                .build();
        return CreateExamAttemptResponse.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .urlSlug(currentExam.getUrlSlug())
                .createdBy(user)
                .createdAt(examAttempt.getCreatedAt().toString())
                .readingExam(listeningExamResponse)
                .build();
    }
}
