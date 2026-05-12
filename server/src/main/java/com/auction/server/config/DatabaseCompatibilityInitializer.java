package com.auction.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCompatibilityInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseCompatibilityInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCompatibilityInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        normalizeRoleValues();
        allowRejectedAuctionStatus();
    }

    private void normalizeRoleValues() {
        runUpdate(
                "Chuẩn hóa role bidder",
                "UPDATE users SET role = 'BIDDER' WHERE LOWER(role) = 'bidder'"
        );
        runUpdate(
                "Chuẩn hóa role seller",
                "UPDATE users SET role = 'SELLER' WHERE LOWER(role) = 'seller'"
        );
    }

    private void allowRejectedAuctionStatus() {
        runStatement(
                "Bổ sung trạng thái REJECTED cho auction_sessions.status",
                "ALTER TABLE auction_sessions MODIFY COLUMN status "
                        + "ENUM('PENDING','ACTIVE','ENDED','CANCELED','REJECTED') DEFAULT NULL"
        );
    }

    private void runUpdate(String action, String sql) {
        try {
            int affectedRows = jdbcTemplate.update(sql);
            logger.info("{} hoàn tất, số dòng ảnh hưởng: {}", action, affectedRows);
        } catch (Exception e) {
            logger.warn("{} bị bỏ qua: {}", action, e.getMessage());
        }
    }

    private void runStatement(String action, String sql) {
        try {
            jdbcTemplate.execute(sql);
            logger.info("{} hoàn tất", action);
        } catch (Exception e) {
            logger.warn("{} bị bỏ qua: {}", action, e.getMessage());
        }
    }
}
