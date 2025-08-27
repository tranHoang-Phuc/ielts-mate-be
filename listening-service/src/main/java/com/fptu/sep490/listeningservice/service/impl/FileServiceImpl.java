package com.fptu.sep490.listeningservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    Cloudinary cloudinary;
    KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topic.gen-transcript}")
    @NonFinal
    String genTranscriptTopic;

    @Value("${topic.upload-audio}")
    @NonFinal
    String uploadAudioTopic;

    @Value("${topic.send-notification}")
    @NonFinal
    String sseEventTopic;

	// For testability: provide an overridable seam for the Cloudinary upload
	protected Map<?, ?> doUpload(String folderName, MultipartFile multipart) throws IOException {
		return cloudinary.uploader()
				.upload(multipart.getBytes(), ObjectUtils.asMap(
						"folder", folderName,
						"resource_type", "auto"
				));
	}

    @Override
    @Async("uploadExecutor")
    public void uploadAsync(String folderName, MultipartFile multipart,
                            UUID taskId, UUID clientId, boolean isAuto) {
        final long initialDelayMs = 500;
        final long maxDelayMs = 30_000;
        final long maxTotalTimeMs = 15 * 60_000; // safety: stop after 15 minutes (tune as needed)

        long attempt = 0;
        long delay = initialDelayMs;
        long start = System.currentTimeMillis();

        while (true) {
            try {
                Map<?, ?> result = doUpload(folderName, multipart);
                AudioFileUpload payload = buildPayload(folderName, taskId, result);
                if (isAuto) kafkaTemplate.send(genTranscriptTopic, payload);
                kafkaTemplate.send(uploadAudioTopic, payload);

                kafkaTemplate.send(sseEventTopic, SseEvent.builder()
                        .clientId(clientId).status("success").message("File uploaded").build());
                return;

            } catch (Exception e) {
                attempt++;
                if (isNonRetryable(e)) {
                    kafkaTemplate.send(sseEventTopic, SseEvent.builder()
                            .clientId(clientId).status("error").message("Non-retryable upload error").build());
                    throw wrapUploadError(e);
                }
                if (System.currentTimeMillis() - start > maxTotalTimeMs) {
                    kafkaTemplate.send(sseEventTopic, SseEvent.builder()
                            .clientId(clientId).status("error")
                            .message("Upload failed after prolonged retries").build());
                    throw wrapUploadError(e);
                }

                // Backoff with jitter
                long jitter = ThreadLocalRandom.current().nextLong(0, delay / 2 + 1);
                long sleepMs = Math.min(maxDelayMs, delay + jitter);

                // Optional: progress/error ping
                kafkaTemplate.send(sseEventTopic, SseEvent.builder()
                        .clientId(clientId).status("retrying")
                        .message("Upload failed, retry " + attempt + " in " + sleepMs + "ms").build());

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw wrapUploadError(ie);
                }

                // Exponential growth
                delay = Math.min(maxDelayMs, delay * 2);
            }
        }
    }

    private boolean isNonRetryable(Exception e) {

        return false;
    }

    private RuntimeException wrapUploadError(Throwable e) {
        return new AppException(
                Constants.ErrorCodeMessage.ERROR_WHEN_UPLOAD,
                Constants.ErrorCode.ERROR_WHEN_UPLOAD,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e
        );
    }

    private AudioFileUpload buildPayload(String folderName, UUID taskId, Map<?, ?> result) {
        String publicId = (String) result.get("public_id");
        Integer version = ((Number) result.get("version")).intValue();
        String format = (String) result.get("format");
        String resourceType = (String) result.get("resource_type");
        String url = (String) result.get("url");
        Integer bytes = ((Number) result.get("bytes")).intValue();
        return AudioFileUpload.builder()
                .taskId(taskId)
                .publicId(publicId)
                .version(version)
                .format(format)
                .resourceType(resourceType)
                .publicUrl(url)
                .folderName(folderName)
                .bytes(bytes)
                .build();
    }

}

