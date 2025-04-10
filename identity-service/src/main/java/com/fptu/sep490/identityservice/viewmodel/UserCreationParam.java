package com.fptu.sep490.identityservice.viewmodel;

public record UserCreationParam(
        String username,
        boolean enabled,
        String email
) {
}
