package com.fptu.sep490.personalservice.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class FeignGlobalConfig {
    /**
     * Timeout settings: connect = 2s, read = 5s
     */
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                2000, TimeUnit.MILLISECONDS,   // connect timeout
                5000, TimeUnit.MILLISECONDS,   // read timeout
                true
        );
    }

    /**
     * Retryer: retry up to 3 times with 100ms interval and 1s max period
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }

    /**
     * Logging level: FULL (headers, body, metadata)
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
