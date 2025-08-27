package com.fptu.sep490.personalservice.viewmodel.request;

import com.fptu.sep490.personalservice.model.enumeration.AttemptSessionMessageType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptSessionMessage {
    private AttemptSessionMessageType messageType;
    private UUID attemptId;
    private String userId;
    private String sessionId;
    private Long timestamp;
    private String message;
}
