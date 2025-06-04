package com.fptu.sep490.readingservice.service.impl;

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
import com.fptu.sep490.readingservice.service.PassageService;
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
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PassageServiceImpl implements PassageService {

    ReadingPassageRepository readingPassageRepository;
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    RedisService redisService;



    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;


    @Override
    public PassageCreationResponse createPassage(PassageCreationRequest passageCreationRequest,
                                                 HttpServletRequest request) throws JsonProcessingException {
        String userId = getUserIdFromToken(request);
        IeltsType ieltsType = safeEnumFromOrdinal(IeltsType.values(), passageCreationRequest.ieltsType());
        PartNumber partNumber = safeEnumFromOrdinal(PartNumber.values(), passageCreationRequest.partNumber());
        Status passageStatus = safeEnumFromOrdinal(Status.values(), passageCreationRequest.passageStatus());

        ReadingPassage readingPassage = ReadingPassage.builder()
                .title(passageCreationRequest.title())
                .ieltsType(ieltsType)
                .partNumber(partNumber)
                .passageStatus(Status.DRAFT)
                .instruction(passageCreationRequest.instruction())
                .content(passageCreationRequest.content())
                .contentWithHighlightKeyword(passageCreationRequest.contentWithHighlightKeywords())
                .passageStatus(passageStatus)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        ReadingPassage saved = readingPassageRepository.save(readingPassage);
        UserProfileResponse createdUserProfileResponse = getUserProfileById(userId);
        UserProfileResponse updatedUserProfileResponse = getUserProfileById(saved.getUpdatedBy());
        return PassageCreationResponse.builder()
                .passageId(saved.getPassageId().toString())
                .ieltsType(saved.getIeltsType().ordinal())
                .partNumber(saved.getPartNumber().ordinal())
                .passageStatus(saved.getPassageStatus().ordinal())
                .title(saved.getTitle())
                .createdBy(UserInformationResponse.builder()
                        .userId(createdUserProfileResponse.id())
                        .lastName(createdUserProfileResponse.lastName())
                        .firstName(createdUserProfileResponse.firstName())
                        .email(createdUserProfileResponse.email())
                        .build())
                .updatedBy(UserInformationResponse.builder()
                        .userId(updatedUserProfileResponse.id())
                        .lastName(updatedUserProfileResponse.lastName())
                        .firstName(updatedUserProfileResponse.firstName())
                        .email(updatedUserProfileResponse.email())
                        .build())
                .createdAt(saved.getCreatedAt().toString())
                .updatedAt(saved.getUpdatedAt().toString())
                .build();
    }

    @Override
    public Page<PassageGetResponse> getPassages(
            int page,
            int size,
            Integer ieltsType,
            Integer status,
            Integer partNumber,
            String questionCategory
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var spec = PassageSpecifications.byConditions(ieltsType, status, partNumber, questionCategory);
        Page<ReadingPassage> pageResult = readingPassageRepository.findAll(spec, pageable);

        return pageResult.map(this::toPassageGetResponse);
    }

    @Override
    public PassageDetailResponse updatePassage(UUID passageId, UpdatedPassageRequest request,
                                               HttpServletRequest httpServletRequest) {
        String userId = getUserIdFromToken(httpServletRequest);
        ReadingPassage entity  = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        if (request.title() != null) {
            entity.setTitle(request.title());
        }

        if (request.ieltsType() != null) {
            int ordinal = request.ieltsType();
            if (ordinal < 0 || ordinal >= IeltsType.values().length) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            entity.setIeltsType(IeltsType.values()[ordinal]);
        }

        if (request.partNumber() != null) {
            int ordinal = request.partNumber();
            if (ordinal < 0 || ordinal >= PartNumber.values().length) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            entity.setPartNumber(PartNumber.values()[ordinal]);
        }

        if (request.content() != null) {
            entity.setContent(request.content());
        }

        if (request.contentWithHighlightKeywords() != null) {
            entity.setContentWithHighlightKeyword(request.contentWithHighlightKeywords());
        }

        if (request.instruction() != null) {
            entity.setInstruction(request.instruction());
        }

        if (request.passageStatus() != null) {
            int ordinal = request.passageStatus();
            if (ordinal < 0 || ordinal >= Status.values().length) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            entity.setPassageStatus(Status.values()[ordinal]);
        }
        entity.setUpdatedBy(userId);
        ReadingPassage updated = readingPassageRepository.save(entity);

        UserProfileResponse createdProfile;
        UserProfileResponse updatedProfile;
        try {
            createdProfile = getUserProfileById(updated.getCreatedBy());
            updatedProfile = getUserProfileById(updated.getUpdatedBy());
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        UserInformationResponse createdByResp = UserInformationResponse.builder()
                .userId(createdProfile.id())
                .firstName(createdProfile.firstName())
                .lastName(createdProfile.lastName())
                .email(createdProfile.email())
                .build();

        UserInformationResponse updatedByResp = UserInformationResponse.builder()
                .userId(updatedProfile.id())
                .firstName(updatedProfile.firstName())
                .lastName(updatedProfile.lastName())
                .email(updatedProfile.email())
                .build();

        return PassageDetailResponse.builder()
                .passageId(updated.getPassageId().toString())
                .title(updated.getTitle())
                .ieltsType(updated.getIeltsType().ordinal())
                .partNumber(updated.getPartNumber().ordinal())
                .content(updated.getContent())
                .contentWithHighlightKeywords(updated.getContentWithHighlightKeyword())
                .instruction(updated.getInstruction())
                .passageStatus(updated.getPassageStatus().ordinal())
                .createdBy(createdByResp)
                .updatedBy(updatedByResp)
                .createdAt(updated.getCreatedAt().toString())
                .updatedAt(updated.getUpdatedAt().toString())
                .build();
    }

    @Override
    public PassageDetailResponse getPassageById(UUID passageId) {
        var readingPassage = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        UserProfileResponse createdByProfile;
        UserProfileResponse updatedByProfile;
        try {
            createdByProfile = getUserProfileById(readingPassage.getCreatedBy());
            updatedByProfile = getUserProfileById(readingPassage.getUpdatedBy());
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
        UserInformationResponse createdBy = UserInformationResponse.builder()
                .userId(createdByProfile.id())
                .lastName(createdByProfile.lastName())
                .firstName(createdByProfile.firstName())
                .email(createdByProfile.email())
                .build();
        UserInformationResponse updatedBy = UserInformationResponse.builder()
                .userId(updatedByProfile.id())
                .lastName(updatedByProfile.lastName())
                .firstName(updatedByProfile.firstName())
                .email(updatedByProfile.email())
                .build();
        return PassageDetailResponse.builder()
                .passageId(readingPassage.getPassageId().toString())
                .title(readingPassage.getTitle())
                .ieltsType(readingPassage.getIeltsType().ordinal())
                .partNumber(readingPassage.getPartNumber().ordinal())
                .content(readingPassage.getContent())
                .contentWithHighlightKeywords(readingPassage.getContentWithHighlightKeyword())
                .instruction(readingPassage.getInstruction())
                .passageStatus(readingPassage.getPassageStatus().ordinal())
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(readingPassage.getCreatedAt().toString())
                .updatedAt(readingPassage.getUpdatedAt().toString())
                .build();
    }

    @Override
    public void deletePassage(UUID passageId) {
        var existingPassage = readingPassageRepository.existsById(passageId);
        if (!existingPassage) {
            throw new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                    Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value());
        }
        readingPassageRepository.deleteById(passageId);
        log.info("Passage with ID {} has been deleted successfully", passageId);
    }

    @Override
    public Page<PassageGetResponse> getActivePassages(int page, int size, Integer ieltsType, Integer partNumber, String questionCategory) {
        Pageable pageable = PageRequest.of(page, size);
        var spec = PassageSpecifications.byConditions(ieltsType, 1,partNumber, questionCategory);
        Page<ReadingPassage> pageResult = readingPassageRepository.findAll(spec, pageable);

        return pageResult.map(this::toPassageGetResponse);
    }


    private PassageGetResponse toPassageGetResponse(ReadingPassage readingPassage) {
        UserProfileResponse createdByProfile;
        UserProfileResponse updatedByProfile;
        try {
            createdByProfile = getUserProfileById(readingPassage.getCreatedBy());
            updatedByProfile = getUserProfileById(readingPassage.getUpdatedBy());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to fetch user profile", e);
        }

        var createdBy = UserInformationResponse.builder()
                .userId(createdByProfile.id())
                .lastName(createdByProfile.lastName())
                .firstName(createdByProfile.firstName())
                .email(createdByProfile.email())
                .build();

        var updatedBy = UserInformationResponse.builder()
                .userId(updatedByProfile.id())
                .lastName(updatedByProfile.lastName())
                .firstName(updatedByProfile.firstName())
                .email(updatedByProfile.email())
                .build();

        return PassageGetResponse.builder()
                .passageId(readingPassage.getPassageId().toString())
                .ieltsType(readingPassage.getIeltsType().ordinal())
                .partNumber(readingPassage.getPartNumber().ordinal())
                .passageStatus(readingPassage.getPassageStatus().ordinal())
                .title(readingPassage.getTitle())
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(readingPassage.getCreatedAt().toString())
                .updatedAt(readingPassage.getUpdatedAt().toString())
                .build();
    }



    private String getUserIdFromToken(HttpServletRequest request) {
        String token = CookieUtils.getCookieValue(request, "Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new AppException(Constants.ErrorCodeMessage.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value());
        }
    }

    private <T extends Enum<T>> T safeEnumFromOrdinal(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        return values[ordinal];
    }

    private UserProfileResponse getUserProfileById(String userId) throws JsonProcessingException {
        String clientToken = getCachedClientToken();
        return keyCloakUserClient.getUserById(realm, "Bearer " + clientToken, userId);

    }

    private String getCachedClientToken() throws JsonProcessingException {
        final String cacheKey = Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN;

        String cachedToken = redisService.getValue(cacheKey, String.class);
        if (cachedToken != null) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "openid");

        KeyCloakTokenResponse tokenResponse = keyCloakTokenClient.requestToken(form, realm);
        String newToken = tokenResponse.accessToken();
        var expiresIn = tokenResponse.expiresIn();
        redisService.saveValue(cacheKey, newToken, Duration.ofSeconds(expiresIn));
        return newToken;
    }
}
