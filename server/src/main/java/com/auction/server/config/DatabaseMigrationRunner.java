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
            logger.info("[MIGRATION] Bắt đầu tự động cập nhật cấu trúc bảng auction_sessions (loại bỏ PENDING và REJECTED)...");
            jdbcTemplate.execute("ALTER TABLE auction_sessions MODIFY COLUMN status ENUM('ACTIVE', 'ENDED', 'CANCELED', 'COMING') NOT NULL;");
            logger.info("[MIGRATION] Cập nhật kiểu dữ liệu cột status thành công (chỉ giữ lại ACTIVE, ENDED, CANCELED, COMING)!");
        } catch (Exception e) {
            logger.warn("[MIGRATION] Bỏ qua cập nhật cột status (bảng đã cập nhật hoặc không dùng ENUM): " + e.getMessage());
        }
    }
}
