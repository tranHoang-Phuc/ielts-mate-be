package com.fptu.sep490.identityservice.viewmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserAccessInfo(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean emailVerified,
        long createdTimestamp,
        boolean enabled,
        boolean totp,
        AccessInfo access
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccessInfo(
            boolean manageGroupMembership,
            boolean view,
            boolean mapRoles,
            boolean impersonate,
            boolean manage
    ) {
    }
}
