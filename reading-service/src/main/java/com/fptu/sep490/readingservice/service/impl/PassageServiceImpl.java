package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
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
import com.fptu.sep490.readingservice.service.PassageService;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.PassageCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;

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
        IeltsType     ieltsType    = safeEnumFromOrdinal(IeltsType.values(), passageCreationRequest.ieltsType());
        PartNumber partNumber   = safeEnumFromOrdinal(PartNumber.values(), passageCreationRequest.partNumber());
        Status passageStatus= safeEnumFromOrdinal(Status.values(), passageCreationRequest.passageStatus());

        ReadingPassage readingPassage = ReadingPassage.builder()
                .title(passageCreationRequest.title())
                .ieltsType(ieltsType)
                .partNumber(partNumber)
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
        return keyCloakUserClient.getUserById(realm, clientToken, userId);

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
