package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
}
