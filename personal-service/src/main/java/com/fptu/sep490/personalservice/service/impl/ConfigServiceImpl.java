package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.StreakEvent;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.model.UserConfig;
import com.fptu.sep490.personalservice.model.json.StreakConfig;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.service.ConfigService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ConfigServiceImpl implements ConfigService {
    ConfigRepository configRepository;
    ObjectMapper objectMapper;

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


        return config;
    }
}
