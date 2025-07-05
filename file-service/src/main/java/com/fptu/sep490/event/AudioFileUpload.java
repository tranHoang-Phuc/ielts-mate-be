package com.fptu.sep490.event;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
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
