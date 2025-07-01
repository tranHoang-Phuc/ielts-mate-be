package com.fptu.sep490.event;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Builder
public class AudioFileUpload {
    private UUID taskId;
    private String publicId;
    private Integer version;
    private String format;
    private String resourceType;
    private String publicUrl;
    Integer bytes;
    String folderName;
}
