package com.fptu.sep490.identityservice.viewmodel;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UserUpdateRequest(
        @NotNull
        @NotEmpty
        String firstName,
        @NotNull
        @NotEmpty
        String lastName
) {
}
