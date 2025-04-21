package com.fptu.sep490.commonlibrary.exceptions;

import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import lombok.Getter;

public class ConflictException extends RuntimeException {
    private final String message;
    @Getter
    private final String businessErrorCode;

    public ConflictException(String errorCode,String businessErrorCode, Object... var2) {
        this.message = MessagesUtils.getMessage(errorCode, var2);
        this.businessErrorCode = businessErrorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
