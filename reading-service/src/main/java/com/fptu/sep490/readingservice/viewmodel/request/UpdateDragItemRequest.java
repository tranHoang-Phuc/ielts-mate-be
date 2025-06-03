package com.fptu.sep490.readingservice.viewmodel.request;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Request body để cập nhật nội dung của DragItem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDragItemRequest {
    @NotBlank(message = "content cannot be blank")
    private String content;
}