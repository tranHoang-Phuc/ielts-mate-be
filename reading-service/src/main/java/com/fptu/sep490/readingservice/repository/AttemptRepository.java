package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Attempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository extends CrudRepository<Attempt, UUID> {
    Optional<Attempt> findById(UUID uuid);
    Page<Attempt> findAll(Specification<Attempt> spec, Pageable pageable);

}
