package com.fptu.sep490.personalservice.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.event.StreakEvent;
import com.fptu.sep490.personalservice.service.ConfigService;
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
public class StreakListener {
    ConfigService configService;
    @KafkaListener(topics = "${kafka.topic.streak}", groupId = "${spring.kafka.consumer.group-id}")
    public void streak(StreakEvent streakEvent) throws JsonProcessingException {
        configService.getOrAddStreakConfig(streakEvent);
    }
}
