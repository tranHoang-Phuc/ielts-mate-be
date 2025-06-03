package com.fptu.sep490.readingservice.viewmodel.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về khi thao tác DragItem (create / update / get).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DragItemResponse {
    private String group_id;
    private String item_id;
    private String content;
}
