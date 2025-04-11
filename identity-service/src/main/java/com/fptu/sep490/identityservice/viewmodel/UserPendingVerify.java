package com.fptu.sep490.identityservice.viewmodel;

public record UserPendingVerify(
        String userId,
        String email
) {
}
