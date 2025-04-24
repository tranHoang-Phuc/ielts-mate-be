package com.fptu.sep490.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailSendingRequest <T> {
    String subject;
    String htmlContent;
    T data;
    RecipientUser recipientUser;
}
