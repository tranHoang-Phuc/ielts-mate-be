package com.fptu.sep490.fileservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class File {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "file_id", updatable = false, nullable = false)
    UUID fileId;
    @Column(name = "public_id", nullable = false)
    String publicId;
    @Column(name = "version", nullable = false)
    Integer version;
    @Column(name = "format", nullable = false)
    String format;
    @Column(name = "resource_type", nullable = false)
    String resourceType;
    @Column(name = "public_url", nullable = false)
    String publicUrl;
    @Column(name = "bytes", nullable = false)
    Integer bytes;
    @Column(name = "folder", nullable = false)
    String folder;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
