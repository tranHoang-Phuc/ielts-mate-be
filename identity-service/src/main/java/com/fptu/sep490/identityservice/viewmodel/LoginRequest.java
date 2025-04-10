package com.fptu.sep490.identityservice.viewmodel;

public record LoginRequest(
        String username,
        String password
) {
}
