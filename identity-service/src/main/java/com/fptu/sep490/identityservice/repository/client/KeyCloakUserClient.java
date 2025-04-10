package com.fptu.sep490.identityservice.repository.client;

import com.fptu.sep490.identityservice.viewmodel.UserCreationParam;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "keycloakUserClient", url = "${keycloak.base-uri}")
public interface KeyCloakUserClient {
    @PostMapping(value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Authorization: Bearer {token}")
    ResponseEntity<?> createUser(@PathVariable String realm,
                                 @RequestHeader("Authorization") String token,
                                 @RequestBody UserCreationParam user);
}
