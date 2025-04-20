package com.fptu.sep490.identityservice.viewmodel;

public record UserAccessInfo(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean emailVerified,
        int createdTimestamp,
        boolean enabled,
        boolean totp,
        AccessInfo access
) {
    public record AccessInfo(
            boolean manageGroupMembership,
            boolean view,
            boolean mapRoles,
            boolean impersonate,
            boolean manage
    ) {
    }
}
