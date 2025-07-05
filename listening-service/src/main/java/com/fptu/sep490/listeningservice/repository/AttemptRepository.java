package com.fptu.sep490.listeningservice.repository;


import com.fptu.sep490.listeningservice.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

}
