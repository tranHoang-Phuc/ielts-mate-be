package com.fptu.sep490.identityservice.viewmodel;

public record UserCreationRequest(
        String password,
        String firstName,
        String lastName,
        String email
) {
}
