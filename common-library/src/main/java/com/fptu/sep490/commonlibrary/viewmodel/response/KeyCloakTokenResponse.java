package com.fptu.sep490.commonlibrary.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeyCloakTokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("expires_in")
        int expiresIn,
        @JsonProperty("refresh_expires_in")
        int refreshExpiresIn,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("not_before_policy")
        String notBeforePolicy,
        @JsonProperty("session_state")
        String sessionState,
        @JsonProperty("scope")
        String scope,
        @JsonProperty("is_creator")
        boolean isCreator
) {
}
