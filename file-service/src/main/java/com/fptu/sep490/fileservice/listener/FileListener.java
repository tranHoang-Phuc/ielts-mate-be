package com.fptu.sep490.fileservice.listener;

import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.event.UpdateTaskEvent;
import com.fptu.sep490.fileservice.model.File;
import com.fptu.sep490.fileservice.repository.FileRepository;
import com.fptu.sep490.fileservice.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class FileListener {
    FileRepository fileRepository;
    KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topic.update-listening-audio}")
    @NonFinal
    String updateListeningAudioTopic;

    @KafkaListener(topics = "${topic.upload-audio}", groupId = "${spring.kafka.consumer.group-id}")
    public void uploadAudioFile(AudioFileUpload audioFileUpload) throws IOException {
        File file = File.builder()
                .publicId(audioFileUpload.getPublicId())
                .version(audioFileUpload.getVersion())
                .format(audioFileUpload.getFormat())
                .resourceType(audioFileUpload.getResourceType())
                .publicUrl(audioFileUpload.getPublicUrl())
                .bytes(audioFileUpload.getBytes())
                .folder(audioFileUpload.getFolderName())
                .build();
        file = fileRepository.save(file);
        UpdateTaskEvent event = UpdateTaskEvent.builder()
                .fileId(file.getFileId())
                .taskId(audioFileUpload.getTaskId())
                .build();
        kafkaTemplate.send(updateListeningAudioTopic, event);
    }
}
