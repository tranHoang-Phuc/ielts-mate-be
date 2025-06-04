package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Attempt;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface AttemptRepository extends CrudRepository<Attempt, UUID> {
}
