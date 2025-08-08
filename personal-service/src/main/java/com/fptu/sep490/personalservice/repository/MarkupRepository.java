package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.Markup;
import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkupRepository extends JpaRepository<Markup, Integer> {
    Optional<Markup> findByAccountIdAndTaskId(UUID accountId, UUID taskId);

    Page<Markup> findAll(Specification<Markup> spec, Pageable pageable);

    @Query(value = "select * from mark_up m where m.account_id = ?1 and task_type = ?2 and practice_type = ?3 ", nativeQuery = true)
    List<Markup> findMarkupByAccountIdAndTaskTypeAndPracticeType(UUID uuid, int taskType, int practiceType);
}
