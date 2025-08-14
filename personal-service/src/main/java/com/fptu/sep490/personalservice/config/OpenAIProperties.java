package com.fptu.sep490.personalservice.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai.api")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIProperties {
    private String key;
    private String url;
    private String model;
}
