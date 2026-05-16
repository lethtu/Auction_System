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
        repairBidSessionForeignKey();
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


    private void repairBidSessionForeignKey() {
        try {
            var wrongForeignKeys = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME "
                            + "FROM information_schema.KEY_COLUMN_USAGE "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'bids' "
                            + "AND COLUMN_NAME = 'session_id' "
                            + "AND REFERENCED_TABLE_NAME IS NOT NULL "
                            + "AND REFERENCED_TABLE_NAME <> 'auction_sessions'",
                    String.class
            );

            for (String foreignKey : wrongForeignKeys) {
                dropForeignKey(foreignKey);
            }

            removeOrphanBids();
            addCorrectBidSessionForeignKey();
        } catch (Exception e) {
            logger.warn("Sửa khóa ngoại bids.session_id bị bỏ qua: {}", e.getMessage());
        }
    }

    private void dropForeignKey(String foreignKey) {
        String safeForeignKey = foreignKey.replace("`", "``");
        runStatement(
                "Xóa khóa ngoại cũ bids.session_id: " + foreignKey,
                "ALTER TABLE bids DROP FOREIGN KEY `" + safeForeignKey + "`"
        );
    }

    private void removeOrphanBids() {
        runUpdate(
                "Xóa bid mồ côi không còn phiên đấu giá hợp lệ",
                "DELETE b FROM bids b "
                        + "LEFT JOIN auction_sessions s ON b.session_id = s.id "
                        + "WHERE b.session_id IS NOT NULL AND s.id IS NULL"
        );
    }

    private void addCorrectBidSessionForeignKey() {
        runStatement(
                "Bổ sung khóa ngoại bids.session_id sang auction_sessions.id",
                "ALTER TABLE bids ADD CONSTRAINT FK_bids_session_auction_sessions "
                        + "FOREIGN KEY (session_id) REFERENCES auction_sessions(id) "
                        + "ON DELETE CASCADE ON UPDATE CASCADE"
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
