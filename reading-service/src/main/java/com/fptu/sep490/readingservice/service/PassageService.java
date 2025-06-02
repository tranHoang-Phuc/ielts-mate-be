package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.PassageCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.PassageGetResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

public interface PassageService {
    PassageCreationResponse createPassage(PassageCreationRequest passageCreationRequest, HttpServletRequest request) throws JsonProcessingException;
    Page<PassageGetResponse> getPassages(int page,
                                         int size,
                                         Integer ieltsType,
                                         Integer status,
                                         Integer partNumber,
                                         String questionCategory) throws JsonProcessingException;
}
