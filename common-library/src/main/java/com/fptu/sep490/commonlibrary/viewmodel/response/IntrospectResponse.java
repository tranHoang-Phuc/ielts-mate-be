package com.fptu.sep490.commonlibrary.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntrospectResponse(
        @JsonProperty("exp")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        int exp,
        @JsonProperty("iat")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        int iat,
        @JsonProperty("jti") String jti,
        @JsonProperty("iss") String iss,
        @JsonProperty("aud") String aud,
        @JsonProperty("sub") String sub,
        @JsonProperty("typ") String typ,
        @JsonProperty("azp") String azp,
        @JsonProperty("sid") String sid,
        @JsonProperty("acr") String acr,

        @JsonProperty("allowed-origins") List<String> allowedOrigins,
        @JsonProperty("realm_access") RealmAccess realmAccess,
        @JsonProperty("resource_access") Map<String, ResourceAccess> resourceAccess,
        @JsonProperty("scope") String scope,

        @JsonProperty("email_verified")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        boolean emailVerified,
        @JsonProperty("name") String name,
        @JsonProperty("preferred_username") String preferredUsername,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        @JsonProperty("email") String email,

        @JsonProperty("client_id") String clientId,
        @JsonProperty("username") String username,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("active") boolean active
) {
    public record RealmAccess(
            @JsonProperty("roles") List<String> roles
    ) {}

    public record ResourceAccess(
            @JsonProperty("roles") List<String> roles
    ) {}
}
