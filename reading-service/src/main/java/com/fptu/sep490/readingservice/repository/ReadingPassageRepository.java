package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingPassage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingPassageRepository extends JpaRepository<ReadingPassage, UUID>,
        JpaSpecificationExecutor<ReadingPassage> {
    Page<ReadingPassage> findAll(Specification<ReadingPassage> spec, Pageable pageable);
    Optional<ReadingPassage> findById(UUID passageId);

    @Query("""
    SELECT p FROM ReadingPassage p
    WHERE p.parent = :parent AND p.isCurrent = true
    """
    )
    Optional<ReadingPassage> findCurrentVersionByParent(ReadingPassage parent);

    @Query("""
        SELECT p FROM ReadingPassage p 
        WHERE (p.passageId = :passageId OR p.parent.passageId = :passageId )AND p.isCurrent = true
    """)
    Optional<ReadingPassage> findCurrentVersionById(UUID passageId);

    @Query("""
    SELECT rp FROM ReadingPassage rp
    WHERE 
        (rp.passageId IN :ids AND rp.isOriginal = true AND rp.isCurrent = true)
        OR
        (rp.parent.passageId IN :ids AND rp.isCurrent = true)
    """)
    List<ReadingPassage> findCurrentVersionsByIds(@Param("ids") List<UUID> ids);

    @Query("""
    SELECT p FROM ReadingPassage p
    WHERE p.passageId = :id OR p.parent.passageId = :id
""")
    List<ReadingPassage> findAllVersion(@Param("id") UUID passageId);
}
