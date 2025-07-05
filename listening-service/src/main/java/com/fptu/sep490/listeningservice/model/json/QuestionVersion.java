package com.fptu.sep490.listeningservice.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVersion {
    private UUID questionId;
    private List<UUID> choiceMapping;
}
