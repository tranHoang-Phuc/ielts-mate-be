package com.fptu.sep490.readingservice.services;
import com.fptu.sep490.readingservice.Dto.AddGroupQuestionRequest;

public interface GroupQuestionService {
    void createGroupQuestion(String passageId, AddGroupQuestionRequest request);

}
