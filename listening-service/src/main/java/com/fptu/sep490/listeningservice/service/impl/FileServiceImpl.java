package com.fptu.sep490.listeningservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    Cloudinary cloudinary;
    KafkaTemplate<String, Object> kafkaTemplate;
    ListeningTaskRepository listeningTaskRepository;

    @Value("${topic.upload-audio}")
    @NonFinal
    String uploadAudioTopic;


    @Override
    @Async("uploadExecutor")
    public void uploadAsync(String folderName, MultipartFile multipart, UUID taskId) throws IOException {
        Map<?, ?> result = cloudinary.uploader()
                .upload(multipart.getBytes(), ObjectUtils.asMap(
                        "folder", folderName,
                        "resource_type", "auto"
                ));

        String publicId = (String) result.get("public_id");
        Integer version = ((Number) result.get("version")).intValue();
        String format = (String) result.get("format");
        String resourceType = (String) result.get("resource_type");
        String url = (String) result.get("url");
        Integer bytes = ((Number) result.get("bytes")).intValue();


        AudioFileUpload audioFileUpload = AudioFileUpload.builder()
                .taskId(taskId)
                .publicId(publicId)
                .version(version)
                .format(format)
                .resourceType(resourceType)
                .publicUrl(url)
                .folderName(folderName)
                .bytes(bytes)
                .build();

        kafkaTemplate.send(uploadAudioTopic, audioFileUpload);

    }
}
