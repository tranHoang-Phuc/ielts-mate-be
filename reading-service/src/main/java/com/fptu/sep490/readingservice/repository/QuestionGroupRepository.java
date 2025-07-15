package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, UUID>,
        JpaSpecificationExecutor<QuestionGroup> {
    @Query("SELECT qg FROM QuestionGroup qg JOIN qg.readingPassage rp WHERE rp.passageId = :passageId")
    List<QuestionGroup> findAllByReadingPassageByPassageId(@Param("passageId") UUID passageId);


    @Query("""
        SELECT qg FROM QuestionGroup qg 
        WHERE (qg.readingPassage.passageId = :passageId AND qg.isCurrent = true) 
            OR 
              (qg.readingPassage.parent.passageId = :passageId AND qg.isCurrent = true)      
    """)
    List<QuestionGroup> findAllCurrentVersionGroupsByPassageId(UUID passageId);

    @Query("""
        SELECT qg FROM QuestionGroup qg JOIN ReadingPassage lt ON qg.readingPassage.passageId = lt.passageId
            WHERE lt.passageId = :passageId AND qg.isOriginal = true and qg.isDeleted = false
    """)
    List<QuestionGroup> findOriginalVersionByTaskId(UUID passageId);

    @Query("""
        SELECT qg From QuestionGroup qg 
            WHERE (qg.groupId = :groupId AND qg.isOriginal = true AND qg.isCurrent = true and qg.isDeleted = false )
            OR (qg.parent.groupId = :groupId and qg.isCurrent = true and qg.isDeleted = false)
    """)
    QuestionGroup findLatestVersionByOriginalId(UUID groupId);

    @Query("""
        SELECT qg FROM QuestionGroup qg
        WHERE qg.groupId IN :ids
        ORDER BY qg.sectionOrder ASC
    """)
    List<QuestionGroup> findAllByIdOrderBySectionOrder(@Param("ids") List<UUID> ids);
}

