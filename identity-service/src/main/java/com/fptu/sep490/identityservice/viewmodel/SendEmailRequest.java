package com.fptu.sep490.identityservice.viewmodel;

public record SendEmailRequest(
        String email,
        String subject,
        String content
) {
}
