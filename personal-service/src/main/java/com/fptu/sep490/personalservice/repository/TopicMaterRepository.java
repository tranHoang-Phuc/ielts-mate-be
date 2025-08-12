package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.TopicMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TopicMaterRepository extends JpaRepository<TopicMaster, Integer> {
    TopicMaster findByTaskId(UUID taskId);

    void deleteByTaskId(UUID taskId);
}
