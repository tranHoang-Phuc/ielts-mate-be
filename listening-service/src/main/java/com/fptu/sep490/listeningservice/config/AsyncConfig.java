package com.fptu.sep490.listeningservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
@EnableScheduling
@Slf4j
public class AsyncConfig {
    
    @Bean("transcriptExecutor")
    public Executor transcriptExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("transcript-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Transcript task rejected, will retry: {}", r.toString());
            throw new RuntimeException("Transcript task rejected");
        });
        executor.initialize();
        return executor;
    }
    
    @Bean("uploadExecutor")
    public Executor uploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("upload-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Upload task rejected, will retry: {}", r.toString());
            throw new RuntimeException("Upload task rejected");
        });
        executor.initialize();
        return executor;
    }
}