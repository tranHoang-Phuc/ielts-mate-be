// reading-service/src/main/java/com/fptu/sep490/readingservice/repository/QuestionGroupRepository.java
package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, UUID>,
        JpaSpecificationExecutor<QuestionGroup> {
    @Query("SELECT qg FROM QuestionGroup qg JOIN qg.readingPassage rp WHERE rp.passageId = :passageId")
    List<QuestionGroup> findAllByReadingPassageByPassageId(@Param("passageId") UUID passageId);
}