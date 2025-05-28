package com.fptu.sep490.identityservice.viewmodel;

import lombok.Builder;

@Builder
public record PasswordChange(
        String oldPassword,
        String newPassword,
        String confirmNewPassword
) {
}
