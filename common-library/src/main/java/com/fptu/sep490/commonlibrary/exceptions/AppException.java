package com.fptu.sep490.commonlibrary.exceptions;

import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import lombok.Getter;

public class AppException extends RuntimeException{
    private final String message;
    @Getter
    private final String businessErrorCode;
    @Getter
    private final int httpStatusCode;

    public AppException(String errorCode, String businessErrorCode, int httpStatusCode, Object... var2) {
        this.message = MessagesUtils.getMessage(errorCode, var2);
        this.businessErrorCode = businessErrorCode;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
