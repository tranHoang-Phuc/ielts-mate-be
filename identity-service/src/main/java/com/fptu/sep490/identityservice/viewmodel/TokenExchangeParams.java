package com.fptu.sep490.identityservice.viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public record TokenExchangeParams(
        String grantType,
        String clientId,
        String clientSecret,
        String scope
) {
}
