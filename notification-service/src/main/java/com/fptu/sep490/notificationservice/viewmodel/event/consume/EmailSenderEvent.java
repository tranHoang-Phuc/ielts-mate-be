package com.fptu.sep490.notificationservice.viewmodel.event.consume;

public record EmailSenderEvent(
        UserProfileResponse userProfileResponse,
        String token,
        String subject,
        String htmlContent

) {
    public record UserProfileResponse(
            String id,
            String username,
            String email,
            String firstName,
            String lastName
    ) {
    }
}
