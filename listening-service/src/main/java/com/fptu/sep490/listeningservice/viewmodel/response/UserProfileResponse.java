package com.fptu.sep490.listeningservice.viewmodel.response;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record UserProfileResponse (
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) implements Serializable {
}