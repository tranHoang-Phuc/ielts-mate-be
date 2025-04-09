package com.fptu.sep490.identityservice.repository.client;

import com.fptu.sep490.identityservice.viewmodel.TokenExchangeParams;
import com.fptu.sep490.identityservice.viewmodel.TokenExchangeResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name ="keycloakTokenClient", url = "${keycloak.auth-server-url}")
public interface KeyCloakTokenClient {
    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = "application/x-www-form-urlencoded")
    TokenExchangeResponse exchangeToken(@QueryMap TokenExchangeParams params,
                                        @PathVariable("realm") String realm);
}
