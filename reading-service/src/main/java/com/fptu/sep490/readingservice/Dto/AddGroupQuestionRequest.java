package com.fptu.sep490.readingservice.Dto;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import lombok.Data;
import java.util.List;

@Data
public class AddGroupQuestionRequest {
    private int section_order;
    private String section_label;
    private String instruction;
    private List<QuestionCreationRequest> questions;
    private List<String> drag_item;
}
