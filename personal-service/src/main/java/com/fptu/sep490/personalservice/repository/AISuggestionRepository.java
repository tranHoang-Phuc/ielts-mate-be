package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.AISuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface AISuggestionRepository extends JpaRepository<AISuggestion, Integer> {
    Optional<AISuggestion> findByCreatedBy(String createdBy);
}
