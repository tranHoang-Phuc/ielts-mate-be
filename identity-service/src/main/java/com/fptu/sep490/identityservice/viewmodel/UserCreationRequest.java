package com.fptu.sep490.identityservice.viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

@Builder
public record UserCreationRequest(
        @NotNull(message = "ERROR_PASSWORD_REQUIRED")
        @Length(min = 8, max = 32, message = "ERROR_INVALID_PASSWORD_LENGTH") String password,
        @NotNull(message = "ERROR_FIRST_NAME_REQUIRED") @Length(min = 2, max = 32, message = "ERROR_INVALID_FIRST_NAME_LENGTH")
        @JsonProperty("first_name")
        String firstName,
        @NotNull(message = "ERROR_LAST_NAME_REQUIRED") @Length(min = 2, max = 32, message = "ERROR_INVALID_LAST_NAME_LENGTH")
        @JsonProperty("last_name")
        String lastName,
        @NotNull(message = "ERROR_EMAIL_REQUIRED") @Email(message = "ERROR_INVALID_EMAIL_FORMAT") String email) {
}
