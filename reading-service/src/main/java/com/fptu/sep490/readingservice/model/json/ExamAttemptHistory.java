package com.fptu.sep490.readingservice.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptHistory {
    private List<UUID> passageId;
    private List<UUID> questionGroupIds;
    private List<UUID> questionIds;
    Map<UUID, List<String>> userAnswers;
}
