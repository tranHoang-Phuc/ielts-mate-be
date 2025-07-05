package com.fptu.sep490.listeningservice.model.json;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptVersion {
    private UUID taskId;
    private Map<UUID, List<QuestionVersion>> groupMappingQuestion;
    private Map<UUID, List<UUID>> groupMappingDragItem;
}
