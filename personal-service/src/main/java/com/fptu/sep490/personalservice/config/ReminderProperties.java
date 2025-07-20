package com.fptu.sep490.personalservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "reminder")
public class ReminderProperties {
    @Value("${reminder.cron}")
    private String cron;

}
