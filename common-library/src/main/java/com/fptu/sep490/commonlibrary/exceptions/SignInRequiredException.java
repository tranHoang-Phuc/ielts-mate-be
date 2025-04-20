package com.fptu.sep490.commonlibrary.exceptions;

import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import lombok.Getter;

public class SignInRequiredException extends RuntimeException {
    private String message;
    @Getter
    private String businessErrorCode;
    public SignInRequiredException(String errorCode, String businessErrorCode,Object... var2) {
        this.message = MessagesUtils.getMessage(errorCode, var2);
        this.businessErrorCode = businessErrorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
