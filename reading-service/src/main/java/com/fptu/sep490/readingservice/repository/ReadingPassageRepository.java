package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingPassage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ReadingPassageRepository extends JpaRepository<ReadingPassage, UUID>,
        JpaSpecificationExecutor<ReadingPassage> {
    Page<ReadingPassage> findAll(Specification<ReadingPassage> spec, Pageable pageabl);
}
