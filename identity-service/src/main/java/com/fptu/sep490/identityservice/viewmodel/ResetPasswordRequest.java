package com.fptu.sep490.identityservice.viewmodel;

import lombok.Builder;

@Builder
public record ResetPasswordRequest(
        String confirmPassword,
        String email,
        String password,
        String token
) {
}
