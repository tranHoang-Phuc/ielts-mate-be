package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.Markup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarkupRepository extends JpaRepository<Markup, Integer> {
    Optional<Markup> findByAccountIdAndTaskId(UUID accountId, UUID taskId);

    Page<Markup> findAll(Specification<Markup> spec, Pageable pageable);
}
