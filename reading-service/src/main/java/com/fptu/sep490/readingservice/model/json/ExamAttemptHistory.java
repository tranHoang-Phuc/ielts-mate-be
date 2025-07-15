package com.fptu.sep490.readingservice.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.sep490.readingservice.viewmodel.response.ExamAttemptGetDetail;
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
    private Map<UUID, List<UUID>> groupMapItems;
    private List<UUID> questionIds;
    private Map<UUID, List<UUID>> questionMapChoices;
    Map<UUID, List<String>> userAnswers;
}
