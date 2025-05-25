package com.fptu.sep490.fileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.fptu.sep490.fileservice", "com.fptu.sep490.commonlibrary"})
@EnableFeignClients(basePackages = {"com.fptu.sep490.fileservice.repository.client"})
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }

}
