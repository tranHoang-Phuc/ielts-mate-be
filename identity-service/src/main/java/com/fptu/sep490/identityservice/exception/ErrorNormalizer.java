package com.fptu.sep490.identityservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.constants.ApiConstant;
import com.fptu.sep490.commonlibrary.exceptions.KeyCloakRuntimeException;
import com.fptu.sep490.commonlibrary.exceptions.SignInRequiredException;
import com.fptu.sep490.commonlibrary.exceptions.UnauthorizedException;
import com.fptu.sep490.identityservice.constants.Constants;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ErrorNormalizer {
    private final ObjectMapper objectMapper;
    private final Map<String, Constants.ErrorCode> errorCodeMap;

    public ErrorNormalizer() {
        objectMapper = new ObjectMapper();
        errorCodeMap = new HashMap<>();
    }
    public KeyCloakRuntimeException handleKeyCloakException(FeignException exception) {
        if (exception.status() == Integer.parseInt(ApiConstant.CODE_401)) {
            throw new UnauthorizedException(Constants.ErrorCode.UNAUTHORIZED);
        }
        return new KeyCloakRuntimeException(Constants.ErrorCode.KEYCLOAK_ERROR);
    }
}
