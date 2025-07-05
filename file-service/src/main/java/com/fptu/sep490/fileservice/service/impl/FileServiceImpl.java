package com.fptu.sep490.fileservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.fileservice.constants.Constants;
import com.fptu.sep490.fileservice.model.File;
import com.fptu.sep490.fileservice.repository.FileRepository;
import com.fptu.sep490.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

@Service
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    Cloudinary cloudinary;
    FileRepository fileRepository;

    @Override
    public File uploadFile(String folderName, MultipartFile multipart) throws IOException {
        Map<?, ?> result = cloudinary.uploader()
                .upload(multipart.getBytes(), ObjectUtils.asMap(
                        "folder", folderName,
                        "resource_type", "auto"
                ));

        String publicId = (String) result.get("public_id");
        Integer version = ((Number) result.get("version")).intValue();
        String format = (String) result.get("format");
        String resourceType = (String) result.get("resource_type");
        String url = (String) result.get("url");
        Integer bytes = ((Number) result.get("bytes")).intValue();

        File entity = File.builder()
                .publicId(publicId)
                .version(version)
                .format(format)
                .resourceType(resourceType)
                .publicUrl(url)
                .bytes(bytes)
                .folder(folderName)
                .build();
        return fileRepository.save(entity);
    }

    @Override
    public byte[] download(UUID id) throws IOException {
        File metadata = fileRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.FILE_NOT_FOUND,
                        Constants.ErrorCode.FILE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        String url = cloudinary.url()
                .resourceType(metadata.getResourceType())
                .version(metadata.getVersion())
                .secure(true)
                .generate(metadata.getPublicId());

        try (InputStream in = new URL(url).openStream()) {
            return in.readAllBytes();
        }
    }

    @Override
    public File getMetadata(UUID id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.FILE_NOT_FOUND,
                        Constants.ErrorCode.FILE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
    }

    @Override
    public void delete(UUID id) throws IOException {
        File metadata = getMetadata(id);
        cloudinary.uploader().destroy(
                metadata.getPublicId(),
                ObjectUtils.asMap("resource_type", metadata.getResourceType())
        );
        fileRepository.deleteById(id);
    }

    @Override
    public File getByPublicUrl(String publicUrl) {
            return fileRepository.findByPublicUrl(publicUrl)
                    .orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.FILE_NOT_FOUND,
                            Constants.ErrorCode.FILE_NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));

    }
}
