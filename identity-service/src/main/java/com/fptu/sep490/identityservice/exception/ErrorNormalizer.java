package com.fptu.sep490.identityservice.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.constants.ApiConstant;
import com.fptu.sep490.commonlibrary.exceptions.KeyCloakRuntimeException;
import com.fptu.sep490.commonlibrary.exceptions.ResourceExistedException;
import com.fptu.sep490.commonlibrary.exceptions.UnauthorizedException;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.viewmodel.KeyCloakError;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ErrorNormalizer {
    private final ObjectMapper objectMapper;
    private final Map<String, String> errorCodeMap;

    public ErrorNormalizer() {
        objectMapper = new ObjectMapper();
        errorCodeMap = new HashMap<>();
        errorCodeMap.put("User exists with same username", Constants.ErrorCode.EXISTED_USERNAME);
        errorCodeMap.put("User exists with same email", Constants.ErrorCode.EXISTED_EMAIL);
        errorCodeMap.put("User name is missing", Constants.ErrorCode.USERNAME_MISSING);
    }
    public KeyCloakRuntimeException handleKeyCloakException(FeignException exception) throws JsonProcessingException {
        if (exception.status() == Integer.parseInt(ApiConstant.CODE_401)) {
            throw new UnauthorizedException(Constants.ErrorCode.UNAUTHORIZED);
        }
        var response = objectMapper.readValue(exception.contentUTF8(), KeyCloakError.class);
        String errorMessage = response.errorMessage();
        String errorCode = errorCodeMap.get(errorMessage);
        if (errorMessage != null && errorCode != null) {
            switch (errorCode) {
                case Constants.ErrorCode.EXISTED_USERNAME -> {
                    throw new ResourceExistedException(Constants.ErrorCode.EXISTED_USERNAME);
                }
                case Constants.ErrorCode.EXISTED_EMAIL -> {
                    throw new ResourceExistedException(Constants.ErrorCode.EXISTED_EMAIL);
                }
                case Constants.ErrorCode.USERNAME_MISSING -> {
                    throw new ResourceNotFoundException(Constants.ErrorCode.USERNAME_MISSING);
                }
            }
        }
        return new KeyCloakRuntimeException(Constants.ErrorCode.KEYCLOAK_ERROR);
    }
}
