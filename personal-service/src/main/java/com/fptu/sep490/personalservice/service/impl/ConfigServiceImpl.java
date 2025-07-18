package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.event.StreakEvent;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.UserConfig;
import com.fptu.sep490.personalservice.model.json.StreakConfig;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.service.ConfigService;
import com.fptu.sep490.personalservice.viewmodel.response.StreakConfigResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ConfigServiceImpl implements ConfigService {
    ConfigRepository configRepository;
    ObjectMapper objectMapper;
    KafkaTemplate<String, Object> kafkaTemplate;
    Helper helper;
    @Value("${kafka.topic.send-notification}")
    @NonFinal
    String notificationTopic;

    private static final String[] TPL_3  = {
            "Nice! You've hit a %d-day streak—3-day milestone unlocked!",
            "Keep it rolling: %d days straight! First 3-day mark achieved."
    };
    private static final String[] TPL_10 = {
            "Awesome! %d days in a row—10-day milestone!",
            "Double digits! Congrats on your 10-day streak (%d days)."
    };
    private static final String[] TPL_30 = {
            "Impressive: %d days of continuous learning—30-day milestone!",
            "1 month strong! %d-day streak reached."
    };
    private static final String[] TPL_90 = {
            "Phenomenal! %d days—90-day milestone. You’re unstoppable!",
            "Quarter‑year streak! %d days straight of learning. Bravo!"
    };
    @Override
    public StreakConfig getOrAddStreakConfig(StreakEvent streakEvent) throws JsonProcessingException {
        LocalDate today = LocalDate.now();
        var streakConfig = configRepository.getConfigByKeyAndAccountId(Constants.Config.TARGET_CONFIG, streakEvent.accountId())
                .orElseGet(() -> {
                    StreakConfig  config =  StreakConfig.builder()
                            .startDate(today)
                            .lastUpdated(today)
                            .currentStreak(1)
                            .build();

                    try {
                        UserConfig userConfig = UserConfig.builder()
                                .configName(Constants.Config.TARGET_CONFIG)
                                .accountId(streakEvent.accountId())
                                .description("Study Streak")
                                .value(objectMapper.writeValueAsString(config))
                                .build();
                        configRepository.save(userConfig);

                    } catch (JsonProcessingException e) {
                        throw new AppException(Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                                Constants.ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                    try {
                        return objectMapper.writeValueAsString(config);
                    } catch (JsonProcessingException e) {
                        throw new AppException(Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                                Constants.ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                });

        StreakConfig config = objectMapper.convertValue(streakConfig, StreakConfig.class);

        // nếu  lastUpdated là 1 ngày ngay trước ngày hôm nay thì check ntnt
        if(!config.getLastUpdated().isEqual(today)) {
            LocalDate last = config.getLastUpdated();
            if (last.isEqual(today.minusDays(1))) {
                config.setCurrentStreak(config.getCurrentStreak() + 1);
            } else {
                config.setCurrentStreak(1);
                config.setLastUpdated(today);
                config.setLastUpdated(today);
            }
        }
        UserConfig userConfig = configRepository.findByConfigNameAndAccountId(Constants.Config.TARGET_CONFIG, streakEvent.accountId());
        userConfig.setValue(objectMapper.writeValueAsString(config));
        configRepository.save(userConfig);
        if(config.getCurrentStreak() == 3 || config.getCurrentStreak() == 10 || config.getCurrentStreak() == 30 || config.getCurrentStreak() == 90) {
            SseEvent sseEvent = SseEvent.builder()
                    .clientId(streakEvent.accountId())
                    .status("streak")
                    .message(buildMessage(config.getCurrentStreak()))
                    .build();
            kafkaTemplate.send(notificationTopic, sseEvent);
        }

        return config;
    }

    @Override
    public StreakConfigResponse getStreak(HttpServletRequest request) {
        String userId = helper.getUserIdFromToken(request);
        var streakConfig = configRepository.getConfigByKeyAndAccountId(Constants.Config.TARGET_CONFIG, UUID.fromString(userId))
                .orElseGet(() -> {
                    StreakConfig config = StreakConfig.builder()
                            .startDate(null)
                            .lastUpdated(null)
                            .currentStreak(0)
                            .build();
                    try {
                        return objectMapper.writeValueAsString(config);
                    } catch (JsonProcessingException e) {
                        throw new AppException(Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                                Constants.ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());

                    }
                });
        StreakConfig config = objectMapper.convertValue(streakConfig, StreakConfig.class);
        return StreakConfigResponse.builder()
                .currentStreak(config.getCurrentStreak())
                .lastUpdated(config.getLastUpdated())
                .startDate(config.getStartDate())
                .build();
    }

    public String buildMessage(int streak) {
        int milestone = 0;
        if (streak % 90 == 0)  milestone = 90;
        else if (streak % 30 == 0) milestone = 30;
        else if (streak % 10 == 0) milestone = 10;
        else if (streak % 3 == 0)  milestone = 3;

        if (milestone == 0) {
            return null;
        }

        String[] tpl;
        switch (milestone) {
            case 90: tpl = TPL_90; break;
            case 30: tpl = TPL_30; break;
            case 10: tpl = TPL_10; break;
            default:  tpl = TPL_3;  break;
        }

        int idx = ThreadLocalRandom.current().nextInt(tpl.length);
        return String.format(tpl[idx], streak);
    }
}
