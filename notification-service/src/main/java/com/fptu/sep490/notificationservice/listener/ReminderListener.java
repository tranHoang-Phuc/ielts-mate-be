package com.fptu.sep490.notificationservice.listener;

import com.fptu.sep490.event.ReminderEvent;
import com.fptu.sep490.notificationservice.service.EmailService;
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
public class ReminderListener {
    EmailService emailService;

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = "${kafka.topic.reminder}")
    public void reminder(ReminderEvent reminderEvent) {
        emailService.sendReminder(reminderEvent);
    }
}
