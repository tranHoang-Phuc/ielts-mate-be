package com.fptu.sep490.identityservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String baseUri;
    private String tokenUri;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String scope;
}
