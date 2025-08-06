package com.fptu.sep490.commonlibrary.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class IntrospectResponse {

    @JsonProperty("exp")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private long exp;

    @JsonProperty("iat")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private long iat;

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("iss")
    private String iss;

    @JsonProperty("aud")
    private List<String> aud;

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("typ")
    private String typ;

    @JsonProperty("azp")
    private String azp;

    @JsonProperty("sid")
    private String sid;

    @JsonProperty("acr")
    private String acr;

    @JsonProperty("allowedorigins")
    private List<String> allowedOrigins;

    @JsonProperty("realm_access")
    private RealmAccess realmAccess;

    @JsonProperty("resource_access")
    private Map<String, ResourceAccess> resourceAccess;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("email_verified")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean emailVerified;

    @JsonProperty("name")
    private String name;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("active")
    private boolean active;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealmAccess {
        @JsonProperty("roles")
        private List<String> roles;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAccess {
        @JsonProperty("roles")
        private List<String> roles;
    }
}
