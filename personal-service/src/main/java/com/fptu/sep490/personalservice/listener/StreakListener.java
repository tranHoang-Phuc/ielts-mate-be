package com.fptu.sep490.personalservice.listener;

import com.fptu.sep490.event.StreakEvent;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StreakListener {

    @KafkaListener(topics = "${kafka.topic.streak}", groupId = "${spring.kafka.consumer.group-id}")
    public void streak(StreakEvent streakEvent) {

    }
}
