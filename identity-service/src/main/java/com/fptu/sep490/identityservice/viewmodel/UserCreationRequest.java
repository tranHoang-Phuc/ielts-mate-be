package com.fptu.sep490.identityservice.viewmodel;

public record UserCreationRequest(
        String username,
        String password,
        String firstName,
        String lastName,
        String email
) {
}
