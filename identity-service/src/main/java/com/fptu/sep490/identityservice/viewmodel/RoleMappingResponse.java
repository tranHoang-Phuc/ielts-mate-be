package com.fptu.sep490.identityservice.viewmodel;

public record RoleMappingResponse(
        String id,
        String name,
        String description,
        boolean composite,
        boolean clientRole,
        String containerId
) {
}
