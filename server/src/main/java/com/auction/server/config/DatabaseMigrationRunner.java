package com.auction.server.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateDatabase() {
        try {
            logger.info("[MIGRATION] Starting automatic schema update for auction_sessions table (removing PENDING and REJECTED)...");
            jdbcTemplate.execute("ALTER TABLE auction_sessions MODIFY COLUMN status ENUM('ACTIVE', 'ENDED', 'PAID', 'CANCELED', 'COMING', 'DRAFT') NOT NULL;");
            logger.info("[MIGRATION] Status column data type updated successfully (keeping only ACTIVE, ENDED, PAID, CANCELED, COMING, DRAFT)!");
        } catch (Exception e) {
            logger.warn("[MIGRATION] Skipped status column update (table already updated or not using ENUM): " + e.getMessage());
        }
    }
}
