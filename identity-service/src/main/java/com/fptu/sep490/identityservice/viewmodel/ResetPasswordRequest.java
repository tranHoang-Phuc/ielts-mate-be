package com.fptu.sep490.identityservice.viewmodel;

public record ResetPasswordRequest(
        String confirmPassword,
        String email,
        String password,
        String token
) {
}
