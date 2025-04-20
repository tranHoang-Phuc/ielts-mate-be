package com.fptu.sep490.commonlibrary.exceptions;

import lombok.Getter;

public class AccessDeniedException extends RuntimeException {
    @Getter
    private final String businessErrorCode;
    public AccessDeniedException(final String message, String businessErrorCode) {
        super(message);
        this.businessErrorCode = businessErrorCode;
    }
}
