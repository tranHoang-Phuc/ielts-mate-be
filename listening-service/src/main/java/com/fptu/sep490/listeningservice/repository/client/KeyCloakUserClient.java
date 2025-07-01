package com.fptu.sep490.listeningservice.repository.client;

import com.fptu.sep490.listeningservice.viewmodel.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "keycloak-user-client", url = "${keycloak.base-uri}")
public interface KeyCloakUserClient {
    @GetMapping(value = "/admin/realms/{realm}/users/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    UserProfileResponse getUserById(@PathVariable("realm") String realm,
                                    @RequestHeader("Authorization") String token,
                                    @PathVariable("userId") String userId);
}
