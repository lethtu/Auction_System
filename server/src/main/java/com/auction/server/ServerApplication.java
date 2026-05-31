package com.auction.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // Enable scheduling
public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        logger.info("Default timezone set to Asia/Ho_Chi_Minh");
    }

    public static void main(String[] args) {
        logger.info("System is starting up");
        SpringApplication.run(ServerApplication.class, args);
        logger.info("System started successfully");
    }
}