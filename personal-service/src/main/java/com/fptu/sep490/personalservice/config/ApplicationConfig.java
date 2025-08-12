package com.fptu.sep490.personalservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class ApplicationConfig {
    @Bean
    public Random random() {
        return new Random();
    }
}
