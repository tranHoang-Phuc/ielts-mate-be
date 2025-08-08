package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.request.MarkupCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.response.MarkUpResponse;
import com.fptu.sep490.personalservice.viewmodel.response.MarkedUpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface MarkupService {
    void addMarkup(HttpServletRequest request, MarkupCreationRequest markup);

    void deleteMarkup(HttpServletRequest request, UUID taskId);

    Page<MarkUpResponse> getMarkup(int page, int size, List<Integer> markupTypeList, List<Integer> taskTypeList, List<Integer> practiceTypeList, HttpServletRequest request);

    MarkedUpResponse getMarkedUpData(String type);
}
