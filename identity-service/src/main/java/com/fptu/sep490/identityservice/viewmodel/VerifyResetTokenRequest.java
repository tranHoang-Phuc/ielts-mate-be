package com.fptu.sep490.identityservice.viewmodel;

import lombok.Builder;

@Builder
public record VerifyResetTokenRequest(
        String email,
        String otp
) {
}
