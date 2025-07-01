package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.ListeningTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ListeningTaskRepository extends JpaRepository<ListeningTask, UUID> {
}
