package com.fptu.sep490.commonlibrary.exceptions;


import com.fptu.sep490.commonlibrary.viewmodel.error.ErrorVm;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String ERROR_LOG_FORMAT = "Error: URI: {}, ErrorCode: {}, Message: {}";
    private static final String INVALID_REQUEST_INFORMATION_MESSAGE = "Request information is not valid";
    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorVm> handleNotFoundException(NotFoundException ex, WebRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 404);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorVm> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();
        return buildErrorResponse(status, INVALID_REQUEST_INFORMATION_MESSAGE, errors, ex, request, 0);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<ErrorVm> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<String> errors = ex.getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + " " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                }).toList();

        return buildErrorResponse(status, INVALID_REQUEST_INFORMATION_MESSAGE, errors, ex, null, status.value());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorVm> handleOtherException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 500);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorVm> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return handleBadRequest(ex, request);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ErrorVm> handleConstraintViolation(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> String.format("%s %s: %s",
                        violation.getRootBeanClass().getName(),
                        violation.getPropertyPath(),
                        violation.getMessage()))
                .toList();

        return buildErrorResponse(status, INVALID_REQUEST_INFORMATION_MESSAGE, errors, ex, null, 0);
    }

    @ExceptionHandler(DuplicatedException.class)
    protected ResponseEntity<ErrorVm> handleDuplicated(DuplicatedException ex) {
        return handleBadRequest(ex, null);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    protected ResponseEntity<ErrorVm> handleInternalServerErrorException(InternalServerErrorException e) {
        log.error("Internal server error exception: ", e);
        String stackTrace = Arrays.stream(e.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
        ErrorVm errorVm = new ErrorVm(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage(), stackTrace);
        return ResponseEntity.internalServerError().body(errorVm);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorVm> handleMissingParams(MissingServletRequestParameterException e) {
        return handleBadRequest(e, null);
    }

    @ExceptionHandler(ResourceExistedException.class)
    public ResponseEntity<ErrorVm> handleResourceExistedException(ResourceExistedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 409);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorVm> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 403);
    }

    @ExceptionHandler(WrongEmailFormatException.class)
    public ResponseEntity<ErrorVm> handleWrongEmailFormatException(WrongEmailFormatException ex, WebRequest request) {
        return handleBadRequest(ex, request);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorVm> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 405);
    }
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorVm> handleAuthorizationDeniedException(AuthorizationDeniedException ex,
                                                                       WebRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 403);
    }

    @ExceptionHandler({SignInRequiredException.class})
    public ResponseEntity<ErrorVm> handleSignInRequired(SignInRequiredException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, null, 401);
    }

    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<ErrorVm> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = ex.getMessage();
        return buildErrorResponse(status, message, null, ex, request, 401);
    }

    @ExceptionHandler({KeyCloakRuntimeException.class})
    public ResponseEntity<ErrorVm> handleKeyCloakRuntimeException(KeyCloakRuntimeException ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();
        return buildErrorResponse(status, message, null, ex, request, 500);
    }

    @ExceptionHandler({Forbidden.class})
    public ResponseEntity<ErrorVm> handleForbidden(NotFoundException ex, WebRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String message = ex.getMessage();
        return buildErrorResponse(status, message, null, ex, request, 403);
    }

    private String getServletPath(WebRequest webRequest) {
        ServletWebRequest servletRequest = (ServletWebRequest) webRequest;
        return servletRequest.getRequest().getServletPath();
    }

    private ResponseEntity<ErrorVm> handleBadRequest(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();

        return buildErrorResponse(status, message, null, ex, request, 400);
    }

    private ResponseEntity<ErrorVm> buildErrorResponse(HttpStatus status, String message, List<String> errors,
                                                       Exception ex, WebRequest request, int statusCode) {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        if (request != null) {
            log.error(ERROR_LOG_FORMAT, this.getServletPath(request), statusCode, message);
        }
        log.error(message, ex);

        if (isDev) {
            String stackTrace = Arrays.stream(ex.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
            return ResponseEntity.status(status).body(new ErrorVm(
                    "error", message, stackTrace
            ));
        } else {
            return ResponseEntity.status(status).body(new ErrorVm(
                    "error", message
            ));
        }
    }
}
