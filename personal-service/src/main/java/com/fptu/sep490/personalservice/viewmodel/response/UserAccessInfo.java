package com.fptu.sep490.personalservice.viewmodel.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder
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
