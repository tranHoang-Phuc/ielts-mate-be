package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ListeningExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ListeningExamRepository extends JpaRepository<ListeningExam, UUID> {

    @Query("""
    SELECT le FROM ListeningExam le
    WHERE le.parent.listeningExamId = :examId
""")
    List<ListeningExam> findAllCurrentByParentId(@Param("examId") UUID examId);}
