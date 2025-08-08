package com.fptu.sep490.readingservice.repository.client;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.viewmodel.response.MarkedUpResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "mark-up-client", url = "${service.personal-service}")
public interface MarkupClient {

    @GetMapping(value = "/internal/marked-up/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<BaseResponse<MarkedUpResponse>> getMarkedUpData(@RequestHeader("Authorization") String accessToken,
                                                        @PathVariable("type") String type);
}
