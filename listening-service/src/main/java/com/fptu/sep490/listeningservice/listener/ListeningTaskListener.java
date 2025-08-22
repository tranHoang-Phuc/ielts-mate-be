package com.fptu.sep490.listeningservice.listener;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.event.UpdateTaskEvent;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.repository.client.AssemblyAIClient;
import com.fptu.sep490.listeningservice.viewmodel.request.GenTranscriptRequest;
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
    AssemblyAIClient assemblyAIClient;

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
        String publicUrl = audioFileUpload.getPublicUrl();

        GenTranscriptRequest requestBody = GenTranscriptRequest.builder()
                .audioUrl(publicUrl)
                .languageCode(Constants.AssemblyAI.LANGUAGE_CODE_EN)
                .speakerLabels(Constants.AssemblyAI.SPEAKER_LABELS)
                .build();

        // send transcriptRequest
        var transcriptRequest = assemblyAIClient.createGenTranscriptRequest(requestBody, assemblyAIApiKey);

        if (transcriptRequest.getStatusCode() == HttpStatus.OK) {
            var transcriptId = transcriptRequest.getBody().id();

                String transcript = waitForTranscript(assemblyAIClient, transcriptId, 5);
                List<ListeningTask> allVersions = listeningTaskRepository.findAllVersion(audioFileUpload.getTaskId());
                for (ListeningTask listeningTask : allVersions) {
                    listeningTask.setTranscription(transcript);
                    log.info("transcript data: {}", transcript);
                }

                listeningTaskRepository.saveAll(allVersions);
                log.info("Generated Transcript for task ID: {} with transcript Id {}", audioFileUpload.getTaskId(), transcriptId);


        } else {
            log.error(MessagesUtils.getMessage(Constants.ErrorCodeMessage.CREATE_GEN_TRANSCRIPT_ERROR));

            throw new AppException(Constants.ErrorCodeMessage.CREATE_GEN_TRANSCRIPT_ERROR,
                    Constants.ErrorCode.CREATE_GEN_TRANSCRIPT_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

    }


    public String waitForTranscript(AssemblyAIClient client, UUID id, int maxSeconds) {
        int slept = 0;
        int stepMs = 3000;
        while (slept <= maxSeconds * 1000) {
            var resp = client.getTranscript(id, assemblyAIApiKey);
            var body = resp.getBody();
            if (body == null) throw new RuntimeException("Empty body from AssemblyAI");
            var status = body.status(); // queued | processing | completed | error
            if ("completed".equalsIgnoreCase(status)) return body.text();
            if ("error".equalsIgnoreCase(status)) {
                throw new AppException(Constants.ErrorCodeMessage.GET_TRANSCRIPT_ERROR,
                        Constants.ErrorCode.GET_TRANSCRIPT_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            try { Thread.sleep(stepMs); } catch (InterruptedException ignored) {}
            slept += stepMs;
        }
        throw new RuntimeException("Timeout waiting transcript: " + id);
    }
}
