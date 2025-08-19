package com.fptu.sep490.identityservice.viewmodel;

import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record ForgotPasswordRequest(
        @Email
        String email
) {
}
