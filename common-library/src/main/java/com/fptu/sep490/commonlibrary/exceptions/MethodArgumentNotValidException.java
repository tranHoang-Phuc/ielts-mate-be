package com.fptu.sep490.commonlibrary.exceptions;

import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import lombok.Getter;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;

public class MethodArgumentNotValidException extends org.springframework.web.bind.MethodArgumentNotValidException {

    @Getter
    private final String customMessage;

    @Getter
    private final String businessErrorCode;

    public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult,
                                                 String errorCode, String businessErrorCode, Object... args)
            throws IllegalArgumentException {
        super(parameter, bindingResult);
        this.customMessage = MessagesUtils.getMessage(errorCode, args);
        this.businessErrorCode = businessErrorCode;
    }

    @Override
    public String getMessage() {
        return this.customMessage;
    }
}
