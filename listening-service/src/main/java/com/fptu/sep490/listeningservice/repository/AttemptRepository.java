package com.fptu.sep490.listeningservice.repository;


import com.fptu.sep490.listeningservice.model.Attempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

    Page<Attempt> findAll(Specification<Attempt> spec, Pageable pageable);
}
