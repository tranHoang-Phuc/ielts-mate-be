package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamAttemptGetDetail(
        @JsonProperty("exam_attempt_id")
        UUID examAttemptId,
        @JsonProperty("estimated_ielts_band")
        Double bandScore,
        @JsonProperty("listening_exam")
        ListeningExamResponse readingExam,
        @JsonProperty("duration")
        Long duration,
        @JsonProperty("total_point")
        Integer totalQuestion,
        @JsonProperty("created_by")
        UserInformationResponse createdBy,
        @JsonProperty("updated_by")
        UserInformationResponse updatedBy,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("updated_at")
        String updatedAt,
        @JsonProperty("answers")
        Map<UUID, List<String>> answers
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ListeningExamResponse(
            @JsonProperty("listening_exam_id")
            UUID listeningExamId,
            @JsonProperty("listening_exam_name")
            String listeningExamName,
            @JsonProperty("listening_exam_description")
            String listeningExamDescription,
            @JsonProperty("url_slug")
            String urlSlug,
            @JsonProperty("listening_task_id_part1")
            ListeningTaskResponse listeningTaskIdPart1,
            @JsonProperty("listening_task_id_part2")
            ListeningTaskResponse listeningTaskIdPart2,
            @JsonProperty("listening_task_id_part3")
            ListeningTaskResponse listeningTaskIdPart3,
            @JsonProperty("listening_task_id_part4")
            ListeningTaskResponse listeningTaskIdPart4
    ) {
        @Builder
        public record ListeningTaskResponse(
                @JsonProperty("task_id")
                UUID taskId,
                @JsonProperty("ielts_type")
                Integer ieltsType,
                @JsonProperty("part_number")
                Integer partNumber,
                @JsonProperty("instruction")
                String instruction,
                @JsonProperty("title")
                String title,
                @JsonProperty("audio_file_id")
                UUID audioFileId,
                @JsonProperty("transcript")
                String transcript,

                @JsonProperty("question_groups")
                List<QuestionGroupAttemptResponse> questionGroups
        ) {
            @Builder
            @JsonInclude(JsonInclude.Include.NON_NULL)
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
                    List<QuestionAttemptResponse> questions,
                    @JsonProperty("drag_items")
                    List<UpdatedQuestionResponse.DragItemResponse> dragItems
            ) {
                @Builder
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public record QuestionAttemptResponse(
                        @JsonProperty("question_id")
                        UUID questionId,
                        @JsonProperty("start_time")
                        String startTime,
                        @JsonProperty("end_time")
                        String endTime,
                        @JsonProperty("question_order")
                        Integer questionOrder,
                        @JsonProperty("question_type")
                        Integer questionType,
                        @JsonProperty("number_of_correct_answers")
                        Integer numberOfCorrectAnswers,
                        @JsonProperty("blank_index")
                        Integer blankIndex,
                        @JsonProperty("instruction_for_choice")
                        String instructionForChoice,
                        @JsonProperty("instruction_for_matching")
                        String instructionForMatching,
                        @JsonProperty("zone_index")
                        Integer zoneIndex,
                        @JsonProperty("choices")
                        List<ChoiceAttemptResponse> choices,
                        @JsonProperty("correct_answer")
                        String correctAnswer,
                        @JsonProperty("correct_answer_for_matching")
                        String correctAnswerForMatching,
                        @JsonProperty("explanation")
                        String explanation,
                        @JsonProperty("point")
                        Integer point
                ) {
                    @Builder
                    @JsonInclude(JsonInclude.Include.NON_EMPTY)
                    public record ChoiceAttemptResponse(
                            @JsonProperty("choice_id")
                            UUID choiceId,
                            @JsonProperty("label")
                            String label,
                            @JsonProperty("content")
                            String content,
                            @JsonProperty("choice_order")
                            Integer choiceOrder,
                            @JsonProperty("is_correct")
                            Boolean isCorrect
                    ) {
                    }
                }
            }
        }
    }
}
