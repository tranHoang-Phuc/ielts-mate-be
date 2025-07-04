package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.repository.ExamAttemptRepository;
import com.fptu.sep490.readingservice.repository.ReadingExamRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.repository.specification.ExamAttemptSpecifications;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.response.*;
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

import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ExamAttemptServiceImpl implements ExamAttemptService {
    // Implementation of methods for ExamAttemptService will go here

    ReadingExamRepository readingExamRepo;
    ReadingPassageRepository passageRepo;
    ExamAttemptRepository examAttemptRepo;
    Helper helper;
    PassageServiceImpl passageServiceImpl;

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
                .orElse(originalExam);

        String userId = helper.getUserIdFromToken(request);
        UserInformationResponse user = helper.getUserInformationResponse(userId);

        //create examAttempt
        ExamAttempt examAttempt = ExamAttempt.builder()
                .duration(null) // Initialize duration to null, will be updated later
                .totalPoint(null) // Initialize totalPoint to null, will be updated later
                .readingExam(currentExam)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        //save examAttempt
        examAttempt = examAttemptRepo.saveAndFlush(examAttempt);

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

    @Transactional(readOnly = true)
    @Override
    public Page<UserGetHistoryExamAttemptResponse> getListExamHistory(
            int page,
            int size,
            String readingExamName,
            String sortBy,
            String sortDirection,
            HttpServletRequest request
    ) {

        Pageable pageable = PageRequest.of(page, size);
        var spec = ExamAttemptSpecifications.byConditions(
                readingExamName,
                sortBy,
                sortDirection,
                helper.getUserIdFromToken(request)
        );

        Page<ExamAttempt> examAttemptsResult = examAttemptRepo.findAll(spec, pageable);

        List<ExamAttempt> examAttempts = examAttemptsResult.getContent();

        List<UserGetHistoryExamAttemptResponse> list = examAttempts.stream().map(examAttempt -> {
            UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptReadingExamResponse readingExamResponse =
                    UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptReadingExamResponse.builder()
                            .readingExamId(examAttempt.getReadingExam().getReadingExamId())
                            .readingExamName(examAttempt.getReadingExam().getExamName())
                            .readingExamDescription(examAttempt.getReadingExam().getExamDescription())
                            .urlSlug(examAttempt.getReadingExam().getUrlSlug())
                            .build();

            return UserGetHistoryExamAttemptResponse.builder()
                    .examAttemptId(examAttempt.getExamAttemptId())
                    .readingExam(readingExamResponse)
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
