package com.fptu.sep490.fileservice.controller;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.fileservice.constants.Constants;
import com.fptu.sep490.fileservice.model.File;
import com.fptu.sep490.fileservice.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class FileController {

    FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public String uploadFile() {
        return "File upload endpoint is not implemented yet.";
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getFileById(@PathVariable UUID id) {
        try {
            File metadata = fileService.getMetadata(id);
            byte[] data = fileService.download(id);

            String format = metadata.getFormat();
            MediaType mediaType;
            if (metadata.getResourceType().equals("image")) {
                mediaType = MediaType.parseMediaType("image/" + format);
            } else if (metadata.getResourceType().equals("video")) {
                mediaType = MediaType.parseMediaType("video/" + format);
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        try {
            fileService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/public")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getFileByPublicUrl(@RequestParam("url") String publicUrl) {
        try {
            File metadata = fileService.getByPublicUrl(publicUrl);
            byte[] data = fileService.download(metadata.getFileId());
            MediaType mediaType = resolveMediaType(metadata);
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(mediaType);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // utility for resolving MIME types
    private MediaType resolveMediaType(File metadata) {
        String format = metadata.getFormat();
        switch (metadata.getResourceType()) {
            case "image": return MediaType.parseMediaType("image/" + format);
            case "video": return MediaType.parseMediaType("video/" + format);
            case "audio": return MediaType.parseMediaType("audio/" + format);
            default: return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
