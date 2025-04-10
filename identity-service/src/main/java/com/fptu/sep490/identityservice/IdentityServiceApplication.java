package com.fptu.sep490.identityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.fptu.sep490.identityservice", "com.fptu.sep490.commonlibrary"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.fptu.sep490.identityservice.repository.client"})
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}

}
