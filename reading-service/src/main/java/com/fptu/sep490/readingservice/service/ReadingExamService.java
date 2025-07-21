package com.fptu.sep490.readingservice.service;

import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamResponse;
import com.fptu.sep490.readingservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ReadingExamService {
    public ReadingExamResponse createReadingExam(ReadingExamCreationRequest readingExamCreationRequest, HttpServletRequest request) throws Exception;

    public ReadingExamResponse updateReadingExam(String readingExamId, ReadingExamCreationRequest readingExamCreationRequest, HttpServletRequest httpServletRequest) throws Exception;

    public ReadingExamResponse getReadingExam(String readingExamId, HttpServletRequest httpServletRequest) throws Exception;

    public ReadingExamResponse deleteReadingExam(String readingExamId, HttpServletRequest httpServletRequest) throws Exception;

    Page<ReadingExamResponse> getAllReadingExamsForCreator(HttpServletRequest httpServletRequest, int page, int size, String sortBy, String sortDirection) throws Exception;

    Page<ReadingExamResponse> getAllReadingExams(HttpServletRequest httpServletRequest, int page, int size, String sortBy, String sortDirection, String keyword) throws Exception;

    List<TaskTitle> getTaskTitle(List<UUID> ids);
}
