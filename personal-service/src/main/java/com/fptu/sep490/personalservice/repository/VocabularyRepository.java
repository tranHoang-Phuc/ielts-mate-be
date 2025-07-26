package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VocabularyRepository extends CrudRepository<Vocabulary, UUID> {
    @Query("""
        SELECT v FROM Vocabulary v
        WHERE v.isDeleted = false
          AND v.createdBy = :userId
          AND (
            :keyword IS NULL OR :keyword = '' OR
            LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(v.context) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(v.meaning) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Vocabulary> searchVocabulary(
            @Param("keyword") String keyword,
            Pageable pageable,
            @Param("userId") String userId
    );

    @Query("""
        SELECT v FROM Vocabulary v
        WHERE v.isDeleted = false
          AND LOWER(v.word) LIKE LOWER(:keyword)
          AND v.createdBy = :userId
    """)
    Vocabulary findByWordAndCreatedBy(
            @Param("keyword") String keyword,
            @Param("userId") String userId
    );
}
