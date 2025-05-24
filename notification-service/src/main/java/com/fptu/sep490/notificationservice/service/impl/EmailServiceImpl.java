package com.fptu.sep490.notificationservice.service.impl;

import com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage;
import com.fptu.sep490.commonlibrary.exceptions.BrevoException;
import com.fptu.sep490.notificationservice.constants.Constants;
import com.fptu.sep490.notificationservice.repository.client.EmailClient;
import com.fptu.sep490.notificationservice.service.EmailService;
import com.fptu.sep490.notificationservice.viewmodel.event.EmailResponse;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.EmailRequest;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.EmailSenderEvent;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.Recipient;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.Sender;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {

    EmailClient emailClient;

    @Value("${notification.email.brevo-apikey}")
    @NonFinal
    String apiKey;

    @Override
    public EmailResponse sendEmail(EmailSenderEvent sendEmailRequest) {
        EmailRequest emailResponse = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("noreply")
                        .email("noreply.ielsmate@gmail.com")
                        .build()
                )
                .to(List.of(Recipient.builder()
                                .email(sendEmailRequest.recipientUser().getEmail())
                                .name(sendEmailRequest.recipientUser().getFirstName().concat(" ")
                                        .concat(sendEmailRequest.recipientUser().getLastName()))
                                .build()))
                .subject(sendEmailRequest.subject())
                .htmlContent(sendEmailRequest.htmlContent())
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailResponse);
        } catch (FeignException e) {
            throw new BrevoException(Constants.ErrorCode.ERROR_WHEN_SENDING_EMAIL,
                    ErrorCodeMessage.ERROR_WHEN_SENDING_EMAIL);
        }
    }

    @Override
    public EmailResponse test() {
        var recipient = Recipient.builder()
                .email("hoangphuc7bdtd@gmail.com")
                .name("Phuc Tran Hoang")
                .build();
        var sender = Sender.builder()
                .email("phucth115.dev@gmail.com")
                .name("noreply")
                .build();

        var emailRequest = EmailRequest.builder()
                .sender(sender)
                .to(List.of(recipient))
                .subject("Test email")
                .htmlContent("<html><body><h1>Hello, this is a test email!</h1></body></html>")
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new BrevoException(Constants.ErrorCode.ERROR_WHEN_SENDING_EMAIL,
                    ErrorCodeMessage.ERROR_WHEN_SENDING_EMAIL);
        }
    }
}
