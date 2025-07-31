package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ExamAttemptAnswersRequest(
        @JsonProperty(value ="passage_id") //truyền 3 passage id vào
        List<UUID> passageId,
        @JsonProperty( value= "question_group_ids") //truyền tất cả group id vào
        List<UUID> questionGroupIds,
        @JsonProperty( value= "item_ids") //tất cả drag item id
        List<UUID> itemsIds,
        @JsonProperty("answers") //truyền câu trả lời user
        List<ExamAnswerRequest> answers,
        @JsonProperty("duration") //thời gian làm bài
        Integer duration
) {
    public record ExamAnswerRequest(
            @JsonProperty("question_id")
            UUID questionId,
            @JsonProperty("selected_answers") //1. multiple choice: mảng các choice id mà user chọn
                                                //2. fill in the blank: ["câu trả lời user điền"]
                                                //3. matching: ["4-A"]
                                                //4. drag and drop: ["item id mà user chọn"]
            List<String> selectedAnswers,
            @JsonProperty("choice_ids") //nếu question là multiple choice thì truyền list tâất cả choice id
            List<UUID>  choiceIds
    ) {
    }
}
