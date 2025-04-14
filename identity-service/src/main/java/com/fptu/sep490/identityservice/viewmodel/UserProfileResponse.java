package com.fptu.sep490.identityservice.viewmodel;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
