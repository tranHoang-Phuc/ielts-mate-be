package com.fptu.sep490.fileservice.repository.client;

import com.fptu.sep490.commonlibrary.viewmodel.response.IntrospectResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "keycloak-token-client", url = "${keycloak.base-uri}")
public interface KeyCloakTokenClient {
    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KeyCloakTokenResponse requestToken(@RequestBody MultiValueMap<String, String> formParams,
                                       @PathVariable("realm") String realm);

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token/introspect",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    IntrospectResponse introspect(@PathVariable("realm") String realm,
                                  @RequestBody MultiValueMap<String, String> formParams);
}
