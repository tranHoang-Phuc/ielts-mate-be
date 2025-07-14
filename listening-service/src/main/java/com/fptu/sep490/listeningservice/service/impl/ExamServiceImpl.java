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

import java.util.List;
import java.util.UUID;


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

    @Override
    public ExamResponse getExamById(String examId, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        ListeningExam listeningExam = listeningExamRepository.findById(UUID.fromString(examId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        ListeningExam currentExam = findCurrentOrChildCurrentExam(listeningExam);

        ExamResponse response = new ExamResponse(
                currentExam.getListeningExamId(),
                currentExam.getExamName(),
                currentExam.getExamDescription(),
                currentExam.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart1().getTaskId())
                        .ieltsType(currentExam.getPart1().getIeltsType().ordinal())
                        .partNumber(currentExam.getPart1().getPartNumber().ordinal())
                        .instruction(currentExam.getPart1().getInstruction())
                        .title(currentExam.getPart1().getTitle())
                        .audioFileId(currentExam.getPart1().getAudioFileId())
                        .transcription(currentExam.getPart1().getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart2().getTaskId())
                        .ieltsType(currentExam.getPart2().getIeltsType().ordinal())
                        .partNumber(currentExam.getPart2().getPartNumber().ordinal())
                        .instruction(currentExam.getPart2().getInstruction())
                        .title(currentExam.getPart2().getTitle())
                        .audioFileId(currentExam.getPart2().getAudioFileId())
                        .transcription(currentExam.getPart2().getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart3().getTaskId())
                        .ieltsType(currentExam.getPart3().getIeltsType().ordinal())
                        .partNumber(currentExam.getPart3().getPartNumber().ordinal())
                        .instruction(currentExam.getPart3().getInstruction())
                        .title(currentExam.getPart3().getTitle())
                        .audioFileId(currentExam.getPart3().getAudioFileId())
                        .transcription(currentExam.getPart3().getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart4().getTaskId())
                        .ieltsType(currentExam.getPart4().getIeltsType().ordinal())
                        .partNumber(currentExam.getPart4().getPartNumber().ordinal())
                        .instruction(currentExam.getPart4().getInstruction())
                        .title(currentExam.getPart4().getTitle())
                        .audioFileId(currentExam.getPart4().getAudioFileId())
                        .transcription(currentExam.getPart4().getTranscription())
                        .build(),

                currentExam.getCreatedBy(),
                currentExam.getCreatedAt(),
                currentExam.getUpdatedBy(),
                currentExam.getUpdatedAt(),
                currentExam.getIsCurrent(),
                currentExam.getVersion(),
                currentExam.getIsOriginal(),
                currentExam.getIsDeleted()
        );
        return response;
    }

    @Override
    public void deleteExam(String examId, HttpServletRequest httpServletRequest) throws Exception {
        ListeningExam exam = listeningExamRepository.findById(UUID.fromString(examId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        List<ListeningExam> list = listeningExamRepository.findAllCurrentByParentId(UUID.fromString(examId));
        for (ListeningExam item:list){
            item.setIsDeleted(true);
            listeningExamRepository.save(item);
        }

    }

    private ListeningExam findCurrentOrChildCurrentExam(ListeningExam listeningExam) {
        if (listeningExam.getIsCurrent() && !listeningExam.getIsDeleted()) {
            return listeningExam;
        }
        for(ListeningExam child : listeningExam.getChildren()) {
            if(child.getIsCurrent() && !child.getIsDeleted()) {
                return child;
            }
        }
        return null;
        }

}
