package com.fptu.sep490.identityservice;

import com.fptu.sep490.commonlibrary.config.CorsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.cors.CorsConfiguration;

@SpringBootApplication(scanBasePackages = {"com.fptu.sep490.identityservice", "com.fptu.sep490.commonlibrary"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.fptu.sep490.identityservice.repository.client"})
@EnableConfigurationProperties({CorsConfig.class})
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}

}
