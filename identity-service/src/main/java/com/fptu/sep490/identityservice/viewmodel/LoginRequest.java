package com.fptu.sep490.identityservice.viewmodel;

import jakarta.validation.constraints.Email;
import lombok.NonNull;
import org.hibernate.validator.constraints.Length;

public record LoginRequest(
        @Email(message = "ERROR_INVALID_EMAIL")
        String email,
        @Length(min = 8, max = 32, message = "ERROR_INVALID_PASSWORD_LENGTH")
        String password
) {
}
