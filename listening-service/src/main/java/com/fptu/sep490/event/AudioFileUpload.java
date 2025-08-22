package com.fptu.sep490.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioFileUpload {
    private UUID taskId;
    private String publicId;
    private Integer version;
    private String format;
    private String resourceType;
    private String publicUrl;
    private Integer bytes;
    private String folderName;
}
