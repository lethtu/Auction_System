package com.auction.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Bật công tắc ở đây
public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
    public static void main(String[] args) {
        logger.info("Hệ thống đang khởi động");
        SpringApplication.run(ServerApplication.class, args);
        logger.info("Hệ thống đã khởi động thành công");
    }
}