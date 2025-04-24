package com.fptu.sep490.identityservice.viewmodel;

public record VerifyResetTokenRequest(
        String email,
        String otp
) {
}
