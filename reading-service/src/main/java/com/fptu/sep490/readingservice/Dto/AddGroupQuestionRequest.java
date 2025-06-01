package com.fptu.sep490.readingservice.Dto;
import lombok.Data;
import java.util.List;

@Data
public class AddGroupQuestionRequest {
    private int section_order;
    private String section_label;
    private String instruction;
    private List<QuestionDto> questions;
    private List<String> drag_item;
}
