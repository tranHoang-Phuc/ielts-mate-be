package com.fptu.sep490.personalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.fptu.sep490.personalservice", "com.fptu.sep490.commonlibrary"})
@EnableFeignClients(basePackages = {"com.fptu.sep490.personalservice.repository.client"})
public class PersonalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalServiceApplication.class, args);
    }

}
