package com.fptu.sep490.readingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.exceptions.InternalServerErrorException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import com.fptu.sep490.readingservice.model.enumeration.PartNumber;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.repository.specification.PassageSpecifications;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedPassageRequest;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface PassageService {
    PassageCreationResponse createPassage(PassageCreationRequest passageCreationRequest, HttpServletRequest request) throws JsonProcessingException;
    Page<PassageGetResponse> getPassages(int page,
                                         int size,
                                         List<Integer> ieltsType,
                                         List<Integer> status,
                                         List<Integer> partNumber,
                                         String questionCategory,
                                         String sortBy,
                                         String sortDirection,
                                         String title,
                                         String createdBy) throws JsonProcessingException;
    PassageDetailResponse updatePassage(UUID passageId, UpdatedPassageRequest request, HttpServletRequest httpServletRequest);

    PassageDetailResponse getPassageById(UUID passageId);


    void deletePassage(UUID passageId);

    Page<PassageGetResponse> getActivePassages(int page,
                                               int size,
                                               List<Integer> ieltsType,
                                               List<Integer> partNumber,
                                               String questionCategory,
                                               String sortBy,
                                               String sortDirection,
                                               String title,
                                               String createdBy);
}
