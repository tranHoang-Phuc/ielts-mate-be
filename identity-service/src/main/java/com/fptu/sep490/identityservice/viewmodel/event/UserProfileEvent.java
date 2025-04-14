package com.fptu.sep490.identityservice.viewmodel.event;

import com.fptu.sep490.identityservice.viewmodel.UserProfileResponse;

public record UserProfileEvent(
        UserProfileResponse userProfileResponse,
        String token,
        String subject,
        String htmlContent
) {
}
