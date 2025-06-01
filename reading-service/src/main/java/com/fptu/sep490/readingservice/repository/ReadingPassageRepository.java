package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.ReadingPassage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReadingPassageRepository extends JpaRepository<ReadingPassage, UUID> {

}
