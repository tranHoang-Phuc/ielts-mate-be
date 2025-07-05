package com.fptu.sep490.listeningservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.fptu.sep490.listeningservice", "com.fptu.sep490.commonlibrary"})
@EnableFeignClients(basePackages = {"com.fptu.sep490.listeningservice.repository.client"})
public class ListeningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListeningServiceApplication.class, args);
    }

}
