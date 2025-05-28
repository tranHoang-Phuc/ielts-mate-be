package com.fptu.sep490.identityservice.viewmodel;

import lombok.Builder;

@Builder
public record UserUpdateRequest(
        String firstName,
        String lastName
) {
}
