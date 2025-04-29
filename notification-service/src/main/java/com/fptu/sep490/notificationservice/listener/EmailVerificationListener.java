package com.fptu.sep490.notificationservice.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.event.EmailSendingRequest;
import com.fptu.sep490.event.VerificationRequest;
import com.fptu.sep490.notificationservice.service.EmailService;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.EmailSenderEvent;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;



@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class EmailVerificationListener {
    EmailService emailService;

    @KafkaListener(topics = "${kafka.topic.user-verification}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(EmailSendingRequest request) {
        EmailSenderEvent emailSenderEvent = EmailSenderEvent.builder()
                .recipientUser(request.getRecipientUser())
                .subject("Verify your email")
                .htmlContent(request.getHtmlContent())
                .build();
        emailService.sendEmail(emailSenderEvent);
    }
}
