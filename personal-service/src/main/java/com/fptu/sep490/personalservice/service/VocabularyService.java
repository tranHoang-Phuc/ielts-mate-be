package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.request.VocabularyRequest;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface VocabularyService {
    VocabularyResponse createVocabulary(@Valid VocabularyRequest vocabularyRequest, HttpServletRequest request) throws Exception;

    VocabularyResponse getVocabularyById(String vocabularyId, HttpServletRequest request) throws Exception;

    void deleteVocabularyById(String vocabularyId, HttpServletRequest request) throws Exception;
}
