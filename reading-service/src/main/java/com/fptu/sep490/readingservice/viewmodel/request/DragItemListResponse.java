package com.fptu.sep490.readingservice.viewmodel.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response trả về danh sách DragItem của một QuestionGroup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DragItemListResponse {
    private String group_id;

    private List<DragItemSummaryResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class DragItemSummaryResponse {
        private String item_id;
        private String item_content;
    }
}
