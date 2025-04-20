package com.fptu.sep490.commonlibrary.viewmodel.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorVm(String status,
                      @JsonProperty("error_code")
                      String errorCode ,
                      String message,
                      String trace) {

    public ErrorVm(String status,String errorCode ,String detail) {
        this(status,errorCode ,detail, null);
    }
}

