package com.auction.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // These 2 annotations above are sufficient for Spring Boot to know this app runs background tasks
}