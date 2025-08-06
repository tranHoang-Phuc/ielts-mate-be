package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.SseEvent;
import com.fptu.sep490.event.StreakEvent;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.helper.UtcConverter;
import com.fptu.sep490.personalservice.model.ReminderConfig;
import com.fptu.sep490.personalservice.model.UserConfig;
import com.fptu.sep490.personalservice.model.enumeration.RecurrenceType;
import com.fptu.sep490.personalservice.model.json.StreakConfig;
import com.fptu.sep490.personalservice.model.json.TargetConfig;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.repository.ReminderConfigRepository;
import com.fptu.sep490.personalservice.service.ConfigService;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigUpdateRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ReminderConfigResponse;
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

import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.fptu.sep490.personalservice.helper.UtcConverter.convertToUtc;


@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {
    ConfigRepository configRepository;
    ObjectMapper objectMapper;
    KafkaTemplate<String, Object> kafkaTemplate;
    ReminderConfigRepository reminderConfigRepository;

    Helper helper;
    @Value("${kafka.topic.send-notification}")
    @NonFinal
    String notificationTopic;


    @Override
    public StreakConfig getOrAddStreakConfig(StreakEvent streakEvent) throws JsonProcessingException {
        LocalDate today = LocalDate.now();
        var streakConfig = configRepository.getConfigByKeyAndAccountId(Constants.Config.TARGET_CONFIG, streakEvent.accountId())
                .orElseGet(() -> {
                    StreakConfig config = StreakConfig.builder()
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
        if (!config.getLastUpdated().isEqual(today)) {
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
        if (config.getCurrentStreak() == Constants.Streak.TPL_3 ||
                config.getCurrentStreak() == Constants.Streak.TPL_10 ||
                config.getCurrentStreak() == Constants.Streak.TPL_30 ||
                config.getCurrentStreak() == Constants.Streak.TPL_90) {
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

        // Lấy dữ liệu config từ DB (kiểu String JSON)
        String streakConfigJson = configRepository
                .getConfigByKeyAndAccountId(Constants.Config.TARGET_CONFIG, UUID.fromString(userId))
                .orElse(null);

        // Nếu không có config -> trả default
        if (streakConfigJson == null) {
            return StreakConfigResponse.builder()
                    .currentStreak(0)
                    .lastUpdated(LocalDate.now())
                    .startDate(LocalDate.now())
                    .build();
        }

        try {
            // Parse JSON thành object
            StreakConfig config = objectMapper.readValue(streakConfigJson, StreakConfig.class);

            return StreakConfigResponse.builder()
                    .currentStreak(config.getCurrentStreak())
                    .lastUpdated(config.getLastUpdated())
                    .startDate(config.getStartDate())
                    .build();

        } catch (JsonProcessingException e) {
            // Nếu dữ liệu JSON trong DB bị lỗi format
            throw new AppException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }


    @Override
    public ReminderConfigResponse getReminder(HttpServletRequest request) {
        UUID userId = UUID.fromString(helper.getUserIdFromToken(request));
        ReminderConfig reminderConfig = (ReminderConfig) reminderConfigRepository.findByAccountId(userId);
        if (reminderConfig == null) return null;
        return ReminderConfigResponse.builder()
                .configId(reminderConfig.getConfigId())
                .email(reminderConfig.getEmail())
                .reminderDate(reminderConfig.getReminderDate())
                .reminderTime(reminderConfig.getReminderTime())
                .enabled(reminderConfig.isEnabled())
                .recurrence(reminderConfig.getRecurrence().ordinal())
                .zone(reminderConfig.getTimeZone())
                .message(reminderConfig.getMessage())
                .build();

    }

    @Override
    public ReminderConfigResponse registerReminder(ReminderConfigCreationRequest reminderConfigCreationRequest, HttpServletRequest request) {
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));
        UtcConverter.UtcResult result = convertToUtc(reminderConfigCreationRequest.timeZone(),
                reminderConfigCreationRequest.reminderDate(), reminderConfigCreationRequest.reminderTime());

        if (reminderConfigRepository.existsByAccountId(accountId)) {
            throw new AppException(Constants.ErrorCodeMessage.REMINDER_CONFIGURED,
                    Constants.ErrorCode.REMINDER_CONFIGURED, HttpStatus.CONFLICT.value());
        }

        ReminderConfig config = ReminderConfig.builder()
                .email(reminderConfigCreationRequest.email())
                .accountId(accountId)
                .message(reminderConfigCreationRequest.message())
                .reminderDate(result.getUtcDates())
                .reminderTime(result.getUtcTime())
                .recurrence(safeEnumFromOrdinal(RecurrenceType.values(), reminderConfigCreationRequest.recurrence()))
                .timeZone(reminderConfigCreationRequest.timeZone())
                .enabled(reminderConfigCreationRequest.enable())
                .build();
        config = reminderConfigRepository.save(config);
        return ReminderConfigResponse.builder()
                .configId(config.getConfigId())
                .email(config.getEmail())
                .reminderDate(config.getReminderDate())
                .reminderTime(config.getReminderTime())
                .enabled(config.isEnabled())
                .zone(config.getTimeZone())
                .recurrence(config.getRecurrence().ordinal())
                .message(config.getMessage())
                .build();

    }

    @Override
    public ReminderConfigResponse updateReminder(ReminderConfigUpdateRequest reminderConfig, HttpServletRequest request) {
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));
        if (!reminderConfigRepository.existsByAccountId(accountId)) {
            throw new AppException(Constants.ErrorCodeMessage.REMINDER_NOT_FOUND,
                    Constants.ErrorCode.REMINDER_NOT_FOUND, HttpStatus.CONFLICT.value());
        }
        UtcConverter.UtcResult result = convertToUtc(reminderConfig.timeZone(),
                reminderConfig.reminderDate(), reminderConfig.reminderTime());
        ReminderConfig config = (ReminderConfig) reminderConfigRepository.findByAccountId(accountId);
        config.setEmail(reminderConfig.email());
        config.setMessage(reminderConfig.message());
        config.setReminderDate(result.getUtcDates());
        config.setReminderTime(result.getUtcTime());
        config.setRecurrence(safeEnumFromOrdinal(RecurrenceType.values(), reminderConfig.recurrence()));
        config.setTimeZone(reminderConfig.timeZone());
        config.setEnabled(reminderConfig.enable());

        config = reminderConfigRepository.save(config);
        return ReminderConfigResponse.builder()
                .configId(config.getConfigId())
                .email(config.getEmail())
                .reminderDate(config.getReminderDate())
                .reminderTime(config.getReminderTime())
                .enabled(config.isEnabled())
                .recurrence(config.getRecurrence().ordinal())
                .zone(config.getTimeZone())
                .message(config.getMessage())
                .build();
    }

    @Override
    public TargetConfig getTarget(HttpServletRequest request) throws JsonProcessingException {
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));
        String targetValue = configRepository.getConfigByKeyAndAccountId(Constants.Config.TARGET_CONFIG, accountId)
                .orElseGet(()->null);
        if(targetValue == null) return null;
        return objectMapper.readValue(targetValue, TargetConfig.class);
    }

    @Override
    public TargetConfig addOrUpdate(HttpServletRequest request, TargetConfig targetConfig) throws JsonProcessingException {
        UUID accountId = UUID.fromString(helper.getUserIdFromToken(request));
        String targetValue = configRepository.getConfigByKeyAndAccountId(Constants.Config.TARGET_CONFIG, accountId)
                .orElseGet(() -> null);
        String value = objectMapper.writeValueAsString(targetConfig);
        if (targetValue == null) {

            UserConfig config = UserConfig.builder()
                    .accountId(accountId)
                    .configName(Constants.Config.TARGET_CONFIG)
                    .value(value)
                    .description("Target config")
                    .build();
            configRepository.save(config);
            return targetConfig;
        } else {
            UserConfig config = configRepository.findByAccountIdAndConfigName(accountId, Constants.Config.TARGET_CONFIG);
            config.setValue(value);
            configRepository.save(config);
            return targetConfig;
        }
    }

    private String buildMessage(int streak) {
        int milestone = 0;
        if (streak % Constants.Streak.TPL_90 == 0) milestone = Constants.Streak.TPL_90;
        else if (streak % Constants.Streak.TPL_30 == 0) milestone = Constants.Streak.TPL_30;
        else if (streak % Constants.Streak.TPL_10 == 0) milestone = Constants.Streak.TPL_10;
        else if (streak % Constants.Streak.TPL_3 == 0) milestone = Constants.Streak.TPL_3;

        if (milestone == 0) {
            return null;
        }

        String[] tpl;
        switch (milestone) {
            case Constants.Streak.TPL_90:
                tpl = Constants.StreakMessage.TPL_90;
                break;
            case Constants.Streak.TPL_30:
                tpl = Constants.StreakMessage.TPL_30;
                break;
            case Constants.Streak.TPL_10:
                tpl = Constants.StreakMessage.TPL_10;
                break;
            default:
                tpl = Constants.StreakMessage.TPL_3;
                break;
        }

        int idx = ThreadLocalRandom.current().nextInt(tpl.length);
        return String.format(tpl[idx], streak);
    }

    private <T extends Enum<T>> T safeEnumFromOrdinal(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        return values[ordinal];
    }


}
