package com.fptu.sep490.commonlibrary.exceptions;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String ERROR_LOG_FORMAT = "Error: URI: {}, ErrorCode: {}, Message: {}";
    private static final String INVALID_REQUEST_INFORMATION_MESSAGE = "Request information is not valid";


}
