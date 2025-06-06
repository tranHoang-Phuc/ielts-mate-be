package com.fptu.sep490.identityservice.viewmodel;

import java.util.List;

public record KeyCloakRoleResponse(
        List<RoleMappingResponse> realmMappings
) {
}
