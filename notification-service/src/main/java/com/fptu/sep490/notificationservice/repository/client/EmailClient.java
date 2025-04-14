package com.fptu.sep490.notificationservice.repository.client;

import com.fptu.sep490.notificationservice.viewmodel.event.EmailResponse;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "email-client", url = "${notification.email.brevo-url}")
public interface EmailClient {
    @PostMapping(value = "/v3/smtp/email", produces = MediaType.APPLICATION_JSON_VALUE)
    EmailResponse sendEmail(@RequestHeader("api-key") String apiKey, EmailRequest emailRequest);
}
