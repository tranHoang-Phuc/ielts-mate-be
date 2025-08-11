package com.fptu.sep490.personalservice.repository.client;


import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.viewmodel.response.UserAccessInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;
import java.util.UUID;
@FeignClient(name = "auth-client", url = "${service.identity-service}")
public interface AuthClient {
    @GetMapping(value = "/auth/get-user-info-by-email", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<UserAccessInfo>> getUserInfoByEmail(
            @RequestParam("email") String email,
            @RequestHeader("Authorization") String token
    );
}
