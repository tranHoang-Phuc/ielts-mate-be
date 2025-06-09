package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReadingExamRepository extends JpaRepository<ReadingExam, UUID> {
}
