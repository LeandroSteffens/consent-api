package com.sensedia.consent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ConsentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsentServiceApplication.class, args);
    }
}
