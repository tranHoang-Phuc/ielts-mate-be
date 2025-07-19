package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.ReminderConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReminderConfigRepository extends JpaRepository<ReminderConfig, Integer> {
    Object findByAccountId(UUID accountId);
}
