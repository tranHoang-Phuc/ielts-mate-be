package com.fptu.sep490.fileservice.repository;

import com.fptu.sep490.fileservice.model.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByPublicUrl(String publicUrl);
}
