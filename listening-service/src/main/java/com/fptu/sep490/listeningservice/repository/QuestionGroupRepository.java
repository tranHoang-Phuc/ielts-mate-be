package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, UUID> {

    @Query("""
        SELECT qg FROM QuestionGroup qg LEFT JOIN ListeningTask lt ON qg.listeningTask.taskId = lt.taskId
            WHERE 
                (qg.groupId IN (select g.groupId FROM QuestionGroup g WHERE g.listeningTask.taskId = :taskId) AND qg.isOriginal = true AND qg.isCurrent = true)
                OR 
                (qg.parent.groupId IN (select g.groupId FROM QuestionGroup g WHERE g.listeningTask.taskId = :taskId) AND qg.isCurrent = true)
    """)
    List<QuestionGroup> findAllLatestVersionByTaskId(@Param("taskId") UUID taskId);

    @Query("""
        SELECT qg FROM QuestionGroup qg JOIN ListeningTask lt ON qg.listeningTask.taskId = lt.taskId
            WHERE lt.taskId = :originalTaskId AND qg.isOriginal = true
    """)
    List<QuestionGroup> findOriginalVersionByTaskId(@Param("originalTaskId") UUID originalTaskId);

    @Query("""
        SELECT qg From QuestionGroup qg 
            WHERE (qg.groupId = :groupId AND qg.isOriginal = true AND qg.isCurrent = true)
            OR (qg.parent.groupId = :groupId and qg.isCurrent = true)
    """)
    QuestionGroup findLatestVersionByOriginalId(@Param("groupId") UUID groupId);
}
