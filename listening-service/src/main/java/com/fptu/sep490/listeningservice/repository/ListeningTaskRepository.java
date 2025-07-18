package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ListeningTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ListeningTaskRepository extends JpaRepository<ListeningTask, UUID> {
    @Query("""
        SELECT lt FROM ListeningTask lt WHERE lt.parent.taskId = :taskId and lt.isDeleted = false 
    """)
    List<ListeningTask> findAllByParentId(@Param("taskId") UUID taskId);

    Page<ListeningTask> findAll(Specification<ListeningTask> spec, Pageable pageable);

    @Query("""
        SELECT lt FROM ListeningTask lt
        WHERE 
            (lt.taskId IN :taskIds AND lt.isOriginal = true AND lt.isCurrent = true and lt.isDeleted = false)
            OR 
            (lt.parent.taskId IN :taskIds AND lt.isCurrent = true and lt.isDeleted = false)
    """)
    List<ListeningTask> findCurrentVersionsByIds(List<UUID> taskIds);

    @Query("""
      SELECT lt
        FROM ListeningTask lt
        LEFT JOIN lt.parent p
       WHERE
         (lt.taskId = :taskId AND lt.isCurrent =true)
             OR
         (p.taskId = :taskId AND lt.isCurrent = true)
       
    """)
    ListeningTask findLastestVersion(@Param("taskId") UUID taskId);
}
