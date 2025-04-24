package com.fptu.sep490.notificationservice.viewmodel.event.consume;

import com.fptu.sep490.event.RecipientUser;
import lombok.Builder;

@Builder
public record EmailSenderEvent(
        RecipientUser recipientUser,
        String token,
        String subject,
        String htmlContent

) {

}
