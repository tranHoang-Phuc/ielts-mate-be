package com.fptu.sep490.listeningservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public interface FileService {
    void uploadAsync(String folderName, MultipartFile multipart, UUID taskId) throws IOException;
}
