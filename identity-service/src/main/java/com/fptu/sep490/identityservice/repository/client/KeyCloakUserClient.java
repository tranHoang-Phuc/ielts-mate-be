package com.fptu.sep490.identityservice.repository.client;

import com.fptu.sep490.identityservice.viewmodel.*;
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
    ResponseEntity<?> createUser(@PathVariable String realm,
                                 @RequestHeader("Authorization") String token,
                                 @RequestBody UserCreationParam user);


    @GetMapping(value = "/admin/realms/{realm}/users/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    UserProfileResponse getUserById(@PathVariable("realm") String realm,
                                    @RequestHeader("Authorization") String token,
                                    @PathVariable("userId") String userId);

    @GetMapping(value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    List<UserAccessInfo> getUserByEmail(@PathVariable("realm") String realm,
                                        @RequestHeader("Authorization") String bearerToken,
                                        @RequestParam("username") String username);

    @PutMapping(value = "/admin/realms/{realm}/users/{userId}/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> resetPassword(@PathVariable("realm") String realm,
                                    @RequestHeader("Authorization") String token,
                                    @PathVariable("userId") String userId,
                                    @RequestBody ChangePasswordRequest changePasswordRequest);

    @PutMapping(value = "/admin/realms/{realm}/users/{userId}",
    consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> verifyEmail(@PathVariable("realm") String realm,
                                  @RequestHeader("Authorization") String token,
                                  @PathVariable("userId") String userId,
                                  @RequestBody VerifyParam verifyParam);
}
