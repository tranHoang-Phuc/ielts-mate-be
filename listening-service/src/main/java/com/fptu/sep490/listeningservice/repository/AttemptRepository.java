package com.fptu.sep490.listeningservice.repository;


import com.fptu.sep490.listeningservice.model.Attempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

    Page<Attempt> findAll(Specification<Attempt> spec, Pageable pageable);

    @Query("SELECT a FROM Attempt a WHERE a.createdBy = :userId AND a.totalPoints IS NOT NULL")
    List<Attempt> findAllByUserId(String userId);

    @Query(value = "select count(*) from attempts a where a.finished_at != null", nativeQuery = true)
    int getNumberOfAttempts();
}
