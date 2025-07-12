package com.fptu.sep490.listeningservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record QuestionGroupResponse(@JsonProperty("group_id")
                                    UUID groupId,
                                    @JsonProperty("listening_task_id")
                                    UUID listeningTaskId,
                                    @JsonProperty("section_order")
                                    Integer sectionOrder,
                                    @JsonProperty("section_label")
                                    String sectionLabel,
                                    @JsonProperty("instruction")
                                    String instruction,
                                    @JsonProperty("created_by")
                                    String createdBy,
                                    @JsonProperty("created_at")
                                    LocalDateTime createdAt,
                                    @JsonProperty("updated_by")
                                    String updatedBy,
                                    @JsonProperty("updated_at")
                                    LocalDateTime updatedAt,
                                    @JsonProperty("is_current")
                                    Boolean isCurrent,
                                    @JsonProperty("is_deleted")
                                    Boolean isDeleted,
                                    @JsonProperty("version")
                                    Integer version,
                                    @JsonProperty("is_original")
                                    Boolean isOriginal,
                                    @JsonProperty("parent_id")
                                    UUID parentId,
                                    @JsonProperty("children")
                                    List<QuestionGroupResponse> children,
                                    @JsonProperty("drag_items")
                                    List<DragItemResponse> dragItems
) {
}
