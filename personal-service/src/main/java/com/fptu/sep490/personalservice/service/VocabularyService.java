package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.request.VocabularyRequest;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

public interface VocabularyService {
    VocabularyResponse createVocabulary(@Valid VocabularyRequest vocabularyRequest, HttpServletRequest request) throws Exception;

    VocabularyResponse getVocabularyById(String vocabularyId, HttpServletRequest request) throws Exception;

    void deleteVocabularyById(String vocabularyId, HttpServletRequest request) throws Exception;

    Page<VocabularyResponse> getAllVocabulary(HttpServletRequest request, int page, int size, String sortBy, String sortDirection, String keyword) throws Exception;

    VocabularyResponse updateVocabulary(String vocabularyId, @Valid VocabularyRequest vocabularyRequest, HttpServletRequest request) throws Exception;
}
