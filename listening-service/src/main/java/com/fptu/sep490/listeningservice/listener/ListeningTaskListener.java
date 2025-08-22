package com.fptu.sep490.listeningservice.listener;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.event.UpdateTaskEvent;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.service.AsyncTranscriptService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ListeningTaskListener {
    ListeningTaskRepository listeningTaskRepository;
    KafkaTemplate<String, Object> kafkaTemplate;
    AsyncTranscriptService asyncTranscriptService;

    @Value("${topic.send-notification}")
    @NonFinal
    String sendNotificationTopic;

    @Value("${assembly-ai.api-key}")
    @NonFinal
    String assemblyAIApiKey;



    @KafkaListener(topics = "${topic.update-listening-audio}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUpdateListeningAudio(UpdateTaskEvent message) {
        ListeningTask listeningTask = listeningTaskRepository.findById(message.taskId()).orElseThrow(
                () -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                )
        );
        listeningTask.setAudioFileId(message.fileId());
        listeningTaskRepository.save(listeningTask);
        log.info("Updated ListeningTask with ID: {}", message.taskId());
        SseEvent sseEvent = SseEvent.builder()
                .clientId(UUID.fromString(listeningTask.getCreatedBy()))
                .status("success")
                .message("Your audio file has been handled successfully.")
                .build();
        kafkaTemplate.send(sendNotificationTopic, sseEvent);
    }

    @KafkaListener(topics = "${topic.gen-transcript}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleGenerateTranscript(AudioFileUpload audioFileUpload) {
        log.info("Received audio file upload for transcript generation: {}", audioFileUpload.getTaskId());

        // Use the new async service to initiate transcript generation
        asyncTranscriptService.initiateTranscriptGeneration(audioFileUpload);
    }

}
