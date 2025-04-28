package com.fptu.sep490.commonlibrary.exceptions;

import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import lombok.Getter;
import lombok.Setter;

public class TooManyRequestException extends RuntimeException {
    @Setter
    private String message;
    @Getter
    private String businessErrorCode;
    public TooManyRequestException(String errorCode, String businessErrorCode,Object... var2) {
        this.message = MessagesUtils.getMessage(errorCode, var2);
        this.businessErrorCode = businessErrorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }


}
