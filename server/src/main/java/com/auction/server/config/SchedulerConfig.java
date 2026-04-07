package com.auction.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Chỉ cần 2 annotation trên là đủ để Spring Boot biết ứng dụng này có chạy ngầm
}