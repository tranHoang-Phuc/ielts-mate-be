package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.ExamRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface ExamService {
    ExamResponse createExam(@Valid ExamRequest request, HttpServletRequest httpServletRequest) throws Exception;

    ExamResponse getExamById(String examId, HttpServletRequest httpServletRequest) throws Exception;

    void deleteExam(String examId, HttpServletRequest httpServletRequest) throws Exception;

    ExamResponse updateExam(String examId, ExamRequest request, HttpServletRequest httpServletRequest) throws Exception;
}
