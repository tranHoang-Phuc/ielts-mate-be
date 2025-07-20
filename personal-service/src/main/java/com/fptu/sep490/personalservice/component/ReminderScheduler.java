package com.fptu.sep490.personalservice.component;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.ReminderEvent;
import com.fptu.sep490.personalservice.config.ReminderProperties;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.model.enumeration.RecurrenceType;
import com.fptu.sep490.personalservice.repository.ReminderConfigRepository;
import com.fptu.sep490.personalservice.service.EmailTemplateService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReminderScheduler {
    ReminderProperties props;
    ReminderConfigRepository reminderConfigRepository;
    KafkaTemplate<String, Object> kafkaTemplate;
    EmailTemplateService emailTemplateService;
    TaskExecutor reminderExecutor;
    @Value("${kafka.topic.reminder}")
    @NonFinal
    String reminderTopic;

    public ReminderScheduler(ReminderProperties props,
                             ReminderConfigRepository reminderConfigRepository,
                             KafkaTemplate<String, Object> kafkaTemplate, EmailTemplateService emailTemplateService,
                             @Qualifier("reminderExecutor") TaskExecutor reminderExecutor) {
        this.props = props;
        this.reminderConfigRepository = reminderConfigRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.emailTemplateService = emailTemplateService;
        this.reminderExecutor = reminderExecutor;
    }

    @Scheduled(cron = "${reminder.cron}", zone = "UTC")
    @Transactional(readOnly = true)
    public void scheduleTask() {
        log.debug("Kích hoạt sendReminderNow theo cron `{}`", props.getCron());
        CompletableFuture.runAsync(() -> {
            try {
                // Lấy thời gian hiện tại của cronjob (24h)
                ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
                String time24h = nowUtc.format(DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime time = LocalTime.parse(time24h, DateTimeFormatter.ofPattern("HH:mm"));

                // query db lấy account cần gửi
                Set<String> emailList = new HashSet<>();
                    // Lấy những email cần gửi trong ngày
                    // Lấy những email cần gửi hằng ngày
                List<String> emailOfDaily = reminderConfigRepository.findDailyEmail(time.toString(), RecurrenceType.DAILY.ordinal(), RecurrenceType.NONE.ordinal());
                    // Lấy những email cần gửi hằng tuần
                List<String> emailOfWeekly = reminderConfigRepository.findWeeklyEmail(time, RecurrenceType.WEEKLY.ordinal());
                    // Lấy những email cần gửi hằng tháng
                List<String> emailOfMonthly = reminderConfigRepository.findMonthlyEmail(time, RecurrenceType.MONTHLY.ordinal());
                    // Lấy những email cần gửi hằng năm
                List<String> emailOfYearly = reminderConfigRepository.findYearlyEmail(time, RecurrenceType.YEARLY.ordinal());
         //       emailList.addAll(emailOfDay);
                emailList.addAll(emailOfDaily);
                emailList.addAll(emailOfWeekly);
                emailList.addAll(emailOfMonthly);
                emailList.addAll(emailOfYearly);
                // đẩy vào trong kafka
                String htmlContent = emailTemplateService.buildReminderTemplate();
                ReminderEvent event = ReminderEvent.builder()
                        .subject("IELTS Mate Reminder")
                        .htmlContent(htmlContent)
                        .email(emailList.stream().toList())
                        .build();
                kafkaTemplate.send(reminderTopic, event);

            } catch (Exception e) {
                log.error(e.getMessage());
                throw new AppException(Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                        Constants.ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());

            }
        }, reminderExecutor);
    }
}
