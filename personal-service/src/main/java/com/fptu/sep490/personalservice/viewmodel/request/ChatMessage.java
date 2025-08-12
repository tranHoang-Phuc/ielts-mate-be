package com.fptu.sep490.personalservice.viewmodel.request;

import com.fptu.sep490.personalservice.model.enumeration.SenderRole;
import com.fptu.sep490.personalservice.model.enumeration.MessageType;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {

    private String Content; // The content of the message
    private String sender; // The sender of the message, e.g., "user" or "assistant"
    private String senderId; // The ID of the sender, which can be a user ID or assistant ID
    private String groupId; // NEW
    private SenderRole senderRole; // The role of the sender, e.g., "user", "assistant", etc.
    private MessageType messageType; // The type of the message, e.g., "text", "image", etc.
    private LocalDateTime sendAt;
}
