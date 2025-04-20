package com.fptu.sep490.identityservice.repository.client;

import com.fptu.sep490.identityservice.viewmodel.UserAccessInfo;
import com.fptu.sep490.identityservice.viewmodel.UserCreationParam;
import com.fptu.sep490.identityservice.viewmodel.UserProfileResponse;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "keycloak-user-client", url = "${keycloak.base-uri}")
public interface KeyCloakUserClient {
    @PostMapping(value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Authorization: Bearer {token}")
    ResponseEntity<?> createUser(@PathVariable String realm,
                                 @RequestHeader("Authorization") String token,
                                 @RequestBody UserCreationParam user);


    @GetMapping(value = "/admin/realms/{realm}/users/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Authorization: Bearer {token}")
    UserProfileResponse getUserById(@PathVariable String realm,
                                    @RequestHeader("Authorization") String token,
                                    @PathVariable String userId);

    @GetMapping(value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Authorization: Bearer {token}")
    List<UserAccessInfo> getUserByEmail(@PathVariable String realm,
                                        @RequestHeader("Authorization") String token,
                                        @RequestParam("username") String username);
}
