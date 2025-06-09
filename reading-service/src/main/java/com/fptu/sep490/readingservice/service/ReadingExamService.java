package com.fptu.sep490.readingservice.service;

import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ReadingExamService {
    public ReadingExamCreationResponse createReadingExam(ReadingExamCreationRequest readingExamCreationRequest, HttpServletRequest request) throws Exception;
}
