package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.enumeration.IeltsType;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.service.FileService;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskCreationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ListeningTaskServiceImpl implements ListeningTaskService {
    ListeningTaskRepository listeningTaskRepository;
    FileService fileService;
    Helper helper;

    @Value("${topic.upload-audio}")
    @NonFinal
    String uploadAudioTopic;

    @Override
    public ListeningTaskCreationResponse createListeningTask(ListeningTaskCreationRequest request,
                                                             HttpServletRequest httpServletRequest) throws IOException {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        MultipartFile audio = request.audioFile();
        if (audio == null || audio.isEmpty() || !audio.getContentType().startsWith("audio/")) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // 2) validate size (e.g. max 10MB)
        long maxBytes = 10 * 1024 * 1024;
        if (audio.getSize() > maxBytes) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.PAYLOAD_TOO_LARGE.value()
            );
        }



        ListeningTask listeningTask = ListeningTask.builder()
                .ieltsType(safeEnumFromOrdinal(IeltsType.values(), request.ieltsType()))
                .partNumber(safeEnumFromOrdinal(PartNumber.values(), request.partNumber()))
                .instruction(request.instruction())
                .title(request.title())
                .transcription(request.isAutomaticTranscription() ? null : request.transcription())
                .isOriginal(true)
                .isCurrent(true)
                .parent(null)
                .createdBy(userId)
                .isDeleted(false)
                .build();

        ListeningTask saved = listeningTaskRepository.save(listeningTask);

        fileService.uploadAsync("listening-tasks", audio, saved.getTaskId());

        return ListeningTaskCreationResponse.builder()
                .taskId(saved.getTaskId())
                .audioFileId(saved.getAudioFileId())
                .ieltsType(saved.getIeltsType().ordinal())
                .partNumber(saved.getPartNumber().ordinal())
                .title(saved.getTitle())
                .instruction(saved.getInstruction())
                .transcription(saved.getTranscription())
                .build();
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
