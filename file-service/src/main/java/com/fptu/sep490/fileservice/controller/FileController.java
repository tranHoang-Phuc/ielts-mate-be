package com.fptu.sep490.fileservice.controller;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.fileservice.constants.Constants;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class FileController {

    @RequestMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public String uploadFile() {
        // Logic for file upload
        throw new AppException(Constants.ErrorCodeMessage.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value());
    }
}
