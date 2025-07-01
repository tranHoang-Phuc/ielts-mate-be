package com.fptu.sep490.fileservice.service;

import com.fptu.sep490.fileservice.model.File;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface FileService {
    File uploadFile(String folderName, MultipartFile multipart) throws IOException;
    byte[] download(UUID id) throws IOException;
    File getMetadata(UUID id);
    void delete(UUID id) throws IOException;

    File getByPublicUrl(String publicUrl);
}
