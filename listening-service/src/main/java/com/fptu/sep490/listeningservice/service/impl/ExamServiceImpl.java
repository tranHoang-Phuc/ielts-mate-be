package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningExam;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.ExamService;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    //CreateExam logic goes here
    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;
    ListeningExamRepository listeningExamRepository;


    Helper helper;


    @Override
    public ExamResponse createExam(ExamRequest request, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningTask part1 = listeningTaskRepository.findById(request.part1Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (part1.getPartNumber() != PartNumber.PART_1) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningTask part2 = listeningTaskRepository.findById(request.part2Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (part2.getPartNumber() != PartNumber.PART_2) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningTask part3 = listeningTaskRepository.findById(request.part3Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (part3.getPartNumber() != PartNumber.PART_3) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        ListeningTask part4 = listeningTaskRepository.findById(request.part4Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (part4.getPartNumber() != PartNumber.PART_4) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningExam listeningExam = ListeningExam.builder()
                .examName(request.examName())
                .examDescription(request.examDescription())
                .urlSlug(request.urlSlug())
                .isCurrent(true)
                .isDeleted(false)
                .isOriginal(true)
                .version(1)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .createdBy(userId)
                .build();

        listeningExamRepository.save(listeningExam);

        ExamResponse response = new ExamResponse(
                listeningExam.getListeningExamId(),
                listeningExam.getExamName(),
                listeningExam.getExamDescription(),
                listeningExam.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(part1.getTaskId())
                        .ieltsType(part1.getIeltsType().ordinal())
                        .partNumber(part1.getPartNumber().ordinal())
                        .instruction(part1.getInstruction())
                        .title(part1.getTitle())
                        .audioFileId(part1.getAudioFileId())
                        .transcription(part1.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(part2.getTaskId())
                        .ieltsType(part2.getIeltsType().ordinal())
                        .partNumber(part2.getPartNumber().ordinal())
                        .instruction(part2.getInstruction())
                        .title(part2.getTitle())
                        .audioFileId(part2.getAudioFileId())
                        .transcription(part2.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(part3.getTaskId())
                        .ieltsType(part3.getIeltsType().ordinal())
                        .partNumber(part3.getPartNumber().ordinal())
                        .instruction(part3.getInstruction())
                        .title(part3.getTitle())
                        .audioFileId(part3.getAudioFileId())
                        .transcription(part3.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(part4.getTaskId())
                        .ieltsType(part4.getIeltsType().ordinal())
                        .partNumber(part4.getPartNumber().ordinal())
                        .instruction(part4.getInstruction())
                        .title(part4.getTitle())
                        .audioFileId(part4.getAudioFileId())
                        .transcription(part4.getTranscription())
                        .build(),

                listeningExam.getCreatedBy(),
                listeningExam.getCreatedAt(),
                listeningExam.getUpdatedBy(),
                listeningExam.getUpdatedAt(),
                listeningExam.getIsCurrent(),
                listeningExam.getVersion(),
                listeningExam.getIsOriginal(),
                listeningExam.getIsDeleted()
        );
        return response;
    }

}
