package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.model.json.QuestionVersion;
import com.fptu.sep490.readingservice.repository.AttemptRepository;
import com.fptu.sep490.readingservice.repository.ExamAttemptRepository;
import com.fptu.sep490.readingservice.repository.ReadingExamRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ExamAttemptServiceImpl implements ExamAttemptService {
    // Implementation of methods for ExamAttemptService will go here

    private final ReadingExamRepository readingExamRepo;
    private final ReadingPassageRepository passageRepo;
    private final ExamAttemptRepository examAttemptRepo;
    private final Helper helper;
    private final PassageServiceImpl passageServiceImpl;

    @Transactional
    @Override
    public CreateExamAttemptResponse createExamAttempt(String urlSlug, HttpServletRequest request) throws JsonProcessingException {
        //find reading exam by urlSlug and isOriginal=true and isDeleted = false, else throw error
        // 1. Tìm original exam
        ReadingExam originalExam = readingExamRepo
                .findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(urlSlug)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                        Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                        )
                );

        //get reading exam has parent.readingExamId = readingExamId and isCurrent=true
        // 2. Lấy bản current, nếu ko có current bắn lỗi
        ReadingExam currentExam = readingExamRepo.findCurrentChildByParentId(originalExam.getReadingExamId())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                        Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        String userId = helper.getUserIdFromToken(request);
        UserInformationResponse user = helper.getUserInformationResponse(userId);

        //create examAttempt
        ExamAttempt examAttempt = ExamAttempt.builder()
                .duration(60)
                .readingExam(currentExam)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        //save examAttempt
        examAttempt = examAttemptRepo.save(examAttempt);

        //create CreateExamAttemptResponse

        CreateExamAttemptResponse.ReadingExamResponse readingExamResponse = CreateExamAttemptResponse.ReadingExamResponse.builder()
                .readingExamId(currentExam.getReadingExamId())
                .readingExamName(currentExam.getExamName())
                .readingExamDescription(currentExam.getExamDescription())
                .urlSlug(currentExam.getUrlSlug())
                .readingPassageIdPart1(passageServiceImpl.fromReadingPassage(currentExam.getPart1().getPassageId().toString()))
                .readingPassageIdPart2(passageServiceImpl.fromReadingPassage(currentExam.getPart2().getPassageId().toString()))
                .readingPassageIdPart3(passageServiceImpl.fromReadingPassage(currentExam.getPart3().getPassageId().toString()))
                .build();

        return CreateExamAttemptResponse.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .urlSlug(currentExam.getUrlSlug())
                .createdBy(user)
                .createdAt(examAttempt.getCreatedAt().toString())
                .readingExam(readingExamResponse)
                .build();

    }



}
