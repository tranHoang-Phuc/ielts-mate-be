package com.fptu.sep490.commonlibrary.viewmodel.error;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorVm(String status, String message, String trace) {

    public ErrorVm(String status, String detail) {
        this(status, detail, null);
    }
}

