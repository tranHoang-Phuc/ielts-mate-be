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

}

