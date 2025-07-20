package com.fptu.sep490.personalservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.event.StreakEvent;
import com.fptu.sep490.personalservice.model.json.StreakConfig;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigUpdateRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ReminderConfigResponse;
import com.fptu.sep490.personalservice.viewmodel.response.StreakConfigResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public interface ConfigService {
    StreakConfig getOrAddStreakConfig( StreakEvent streakEvent) throws JsonProcessingException;

    StreakConfigResponse getStreak(HttpServletRequest request);

    ReminderConfigResponse getReminder(HttpServletRequest request);

    ReminderConfigResponse registerReminder(ReminderConfigCreationRequest reminderConfigCreationRequest, HttpServletRequest request);

    ReminderConfigResponse updateReminder(ReminderConfigUpdateRequest reminderConfig, HttpServletRequest request);
}
