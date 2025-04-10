package com.fptu.sep490.identityservice.viewmodel;


import lombok.Builder;

import java.util.List;
@Builder
public record UserCreationParam(
        String username,
        boolean enabled,
        String email,
        boolean emailVerified,
        String firstName,
        String lastName,
        List<Credential> credentials
) {
    @Builder
    public record Credential(
            String type,
            String value,
            boolean temporary
    ) {
    }
}
