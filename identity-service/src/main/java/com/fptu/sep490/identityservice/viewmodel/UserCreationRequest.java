package com.fptu.sep490.identityservice.viewmodel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record UserCreationRequest(
        @NotNull(message = "ERROR_PASSWORD_REQUIRED") @Length(min = 8, max = 32, message = "ERROR_INVALID_PASSWORD_LENGTH") String password,
        @NotNull(message = "ERROR_FIRST_NAME_REQUIRED") @Length(min = 2, max = 32, message = "ERROR_INVALID_FIRST_NAME_LENGTH") String firstName,
        @NotNull(message = "ERROR_LAST_NAME_REQUIRED") @Length(min = 2, max = 32, message = "ERROR_INVALID_LAST_NAME_LENGTH") String lastName,
        @NotNull(message = "ERROR_EMAIL_REQUIRED") @Email(message = "ERROR_INVALID_EMAIL_FORMAT") String email) {
}
