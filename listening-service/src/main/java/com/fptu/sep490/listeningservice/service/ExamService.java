package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.ExamRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.SlugGenResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.SlugStatusResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ExamService {
    ExamResponse createExam(@Valid ExamRequest request, HttpServletRequest httpServletRequest) throws Exception;

    ExamResponse getExamById(String examId, HttpServletRequest httpServletRequest) throws Exception;

    void deleteExam(String examId, HttpServletRequest httpServletRequest) throws Exception;

    ExamResponse updateExam(String examId, ExamRequest request, HttpServletRequest httpServletRequest) throws Exception;

    Page<ExamResponse> getAllExamsForCreator(HttpServletRequest httpServletRequest, int page, int size, String sortBy, String sortDirection, String keyword) throws Exception;

    Page<ExamResponse> getActiveExams(int page, int size, String sortBy, String sortDirection, HttpServletRequest httpServletRequest, String keyword) throws Exception;

    List<TaskTitle> getExamTitle(List<UUID> ids);

    SlugStatusResponse checkUrlSlug(String urlSlug);

    SlugGenResponse genUrlSlug(String examName);
}
