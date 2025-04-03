package com.fptu.sep490.commonlibrary.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
public record BaseResponse<T>(
        String status,
        String message,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) Pagination pagination
) {
    public BaseResponse {
        if (status == null) {
            status = "success";
        }
    }
}
