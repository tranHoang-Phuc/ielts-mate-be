package com.fptu.sep490.notificationservice.listener;

import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.notificationservice.service.SseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class SseListener {

    SseService sseService;

    @KafkaListener(topics = "${kafka.topic.send-notification}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleSseEvent(SseEvent message) {
        sseService.sendMessage(message.clientId(), message.message(), message.status());
        log.info("Sent SSE message to client {}: {}", message.clientId(), message.message());
    }
}
