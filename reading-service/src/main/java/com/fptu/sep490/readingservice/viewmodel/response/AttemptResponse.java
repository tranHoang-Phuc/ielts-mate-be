package com.fptu.sep490.readingservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AttemptResponse(
        @JsonProperty("attempt_id")
        UUID attemptId,
        @JsonProperty("reading_passage_id")
        UUID readingPassageId,
        @JsonProperty("ielts_type")
        int ieltsType,
        @JsonProperty("part_number")
        int partNumber,
        @JsonProperty("instruction")
        String instruction,
        @JsonProperty("content")
        String content,
        @JsonProperty("question_groups")
        List<QuestionGroupAttemptResponse> questionGroups
) {
    @Builder
    public record QuestionGroupAttemptResponse(
            @JsonProperty("question_group_id")
            UUID questionGroupId,
            @JsonProperty("section_order")
            Integer sectionOrder,
            @JsonProperty("section_label")
            String sectionLabel,
            @JsonProperty("instruction")
            String instruction,
            @JsonProperty("sentence_with_blanks")
            String sentenceWithBlanks,
            @JsonProperty("questions")
            List<QuestionAttemptResponse> questions
    ) {
        @Builder
        public record QuestionAttemptResponse(
                @JsonProperty("question_id")
                UUID questionId,
                @JsonProperty("question_order")
                Integer questionOrder,
                @JsonProperty("question_type")
                Integer questionType,
                @JsonProperty("number_of_correct_answers")
                Integer numberOfCorrectAnswers,
                @JsonProperty("blank_index")
                Integer blankIndex,
                @JsonProperty("instruction_for_matching")
                String instructionForMatching,
                @JsonProperty("zone_index")
                Integer zoneIndex,
                @JsonProperty("choices")
                List<ChoiceAttemptResponse> choices
        ) {
            @Builder
            public record ChoiceAttemptResponse(
                    @JsonProperty("choice_id")
                    UUID choiceId,
                    @JsonProperty("label")
                    String label,
                    @JsonProperty("content")
                    String content,
                    @JsonProperty("choice_order")
                    Integer choiceOrder
            ) {
            }
        }
    }
}
