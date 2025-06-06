package com.fptu.sep490.identityservice.viewmodel;

import lombok.Builder;

import java.util.List;

@Builder
public record UserProfileMappingRoles(
        String id,
        String email,
        String firstName,
        String lastName,
        List<String> roles
) {
}
