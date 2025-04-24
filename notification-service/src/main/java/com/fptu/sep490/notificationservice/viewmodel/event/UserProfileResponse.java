package com.fptu.sep490.notificationservice.viewmodel.event;

import java.io.Serializable;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) implements Serializable {
}
