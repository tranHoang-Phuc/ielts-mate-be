package com.fptu.sep490.readingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.fptu.sep490.readingservice", "com.fptu.sep490.commonlibrary"})
public class ReadingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadingServiceApplication.class, args);
    }

}
