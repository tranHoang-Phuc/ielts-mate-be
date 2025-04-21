package com.fptu.sep490.identityservice.viewmodel;

import lombok.Builder;

@Builder
public record ChangePasswordRequest(
        String type,
        boolean temporary,
        String value
) {

}
