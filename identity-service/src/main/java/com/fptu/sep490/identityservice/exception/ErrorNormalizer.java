package com.fptu.sep490.identityservice.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.constants.ApiConstant;
import com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.exceptions.KeyCloakRuntimeException;
import com.fptu.sep490.commonlibrary.exceptions.ResourceExistedException;
import com.fptu.sep490.commonlibrary.exceptions.UnauthorizedException;
import com.fptu.sep490.identityservice.constants.Constants;
import com.fptu.sep490.identityservice.viewmodel.KeyCloakError;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ErrorNormalizer {

    private final ObjectMapper objectMapper;
    private final Map<String, String> errorCodeMap = Map.of(
            "User exists with same username", Constants.ErrorCodeMessage.EXISTED_USERNAME,
            "User exists with same email", Constants.ErrorCodeMessage.EXISTED_EMAIL,
            "User name is missing", Constants.ErrorCodeMessage.USERNAME_MISSING,
            "Account is not fully set up", Constants.ErrorCodeMessage.EMAIL_NOT_VERIFIED
    );

    public ErrorNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KeyCloakRuntimeException handleKeyCloakException(FeignException exception) throws JsonProcessingException {
        if(exception.status() == HttpStatus.UNAUTHORIZED.value()) {
            throw new AppException(Constants.ErrorCodeMessage.WRONG_PASSWORD, Constants.ErrorCode.WRONG_PASSWORD,
                    HttpStatus.UNAUTHORIZED.value());
        }
        log.error(exception.getMessage(), exception);
        var response = objectMapper.readValue(exception.contentUTF8(), KeyCloakError.class);
        String errorMessage = response.errorMessage();
        String errorDescription = response.errorDescription();
        if (errorDescription != null) {
            String code = errorCodeMap.get(errorDescription);
            log.error(errorDescription, code);
            if (Constants.ErrorCodeMessage.EMAIL_NOT_VERIFIED.equals(code)) {
                throw new UnauthorizedException(Constants.ErrorCodeMessage.EMAIL_NOT_VERIFIED, Constants.ErrorCode.EMAIL_NOT_VERIFIED);
            }
        }
        String code = errorCodeMap.get(errorMessage);
        if (code != null) {
            return switch (code) {
                case Constants.ErrorCodeMessage.EXISTED_USERNAME -> throw new ResourceExistedException(code,
                        Constants.ErrorCode.EXISTED_USERNAME);
                case Constants.ErrorCodeMessage.EXISTED_EMAIL -> throw new ResourceExistedException(code,
                        Constants.ErrorCode.EXISTED_EMAIL);
                case Constants.ErrorCodeMessage.USERNAME_MISSING -> throw new ResourceNotFoundException(code,
                        Constants.ErrorCode.USERNAME_MISSING);
                default -> new KeyCloakRuntimeException(Constants.ErrorCodeMessage.KEYCLOAK_ERROR, Constants.ErrorCodeMessage.KEYCLOAK_ERROR);
            };
        }
        return new KeyCloakRuntimeException(Constants.ErrorCodeMessage.KEYCLOAK_ERROR, Constants.ErrorCode.KEYCLOAK_ERROR);
    }
}
