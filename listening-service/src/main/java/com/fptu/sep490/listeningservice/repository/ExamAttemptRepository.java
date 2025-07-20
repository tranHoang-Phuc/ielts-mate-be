package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

}
