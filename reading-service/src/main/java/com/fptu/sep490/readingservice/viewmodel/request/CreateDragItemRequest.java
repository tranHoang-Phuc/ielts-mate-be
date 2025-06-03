package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request body để tạo mới một DragItem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDragItemRequest {

    @NotBlank(message = "content cannot be blank")
    private String content;

    @JsonProperty("question_id")
    private String questionId;
}
