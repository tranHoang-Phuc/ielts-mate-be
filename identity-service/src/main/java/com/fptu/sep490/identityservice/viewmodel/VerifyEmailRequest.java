package com.fptu.sep490.identityservice.viewmodel;

public record VerifyEmailRequest(
        String email,
        String otp
) {
}
