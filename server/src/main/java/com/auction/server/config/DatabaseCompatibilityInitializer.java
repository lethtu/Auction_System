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
        relaxLegacyDtypeColumn();
        normalizeRoleValues();
        allowRejectedAuctionStatus();
        ensureItemHiddenColumn();
        repairBidSessionForeignKey();
    }

    private void relaxLegacyDtypeColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) "
                            + "FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'users' "
                            + "AND COLUMN_NAME = 'dtype'",
                    Integer.class
            );

            if (count == null || count == 0) {
                return;
            }

            runStatement(
                    "Cho phep bo trong cot users.dtype cu",
                    "ALTER TABLE users MODIFY COLUMN dtype VARCHAR(31) NULL DEFAULT NULL"
            );
            runUpdate(
                    "Dong bo dtype cu theo role",
                    "UPDATE users SET dtype = role WHERE dtype IS NULL AND role IS NOT NULL"
            );
        } catch (Exception e) {
            logger.warn("Sua cot users.dtype cu bi bo qua: {}", e.getMessage());
        }
    }

    private void normalizeRoleValues() {
        runUpdate(
                "Chuẩn hóa role bidder",
                "UPDATE users SET role = 'bidder' "
                        + "WHERE id >= 0 AND role IS NOT NULL AND LOWER(role) = 'bidder'"
        );
        runUpdate(
                "Chuẩn hóa role seller",
                "UPDATE users SET role = 'seller' "
                        + "WHERE id >= 0 AND role IS NOT NULL AND LOWER(role) = 'seller'"
        );
        runUpdate(
                "Chuẩn hóa role admin",
                "UPDATE users SET role = 'admin' "
                        + "WHERE id >= 0 AND role IS NOT NULL AND LOWER(role) = 'admin'"
        );
        runUpdate(
                "Chuẩn hóa role user",
                "UPDATE users SET role = 'user' "
                        + "WHERE id >= 0 AND (role IS NULL OR TRIM(role) = '' OR LOWER(role) = 'user')"
        );
    }

    private void allowRejectedAuctionStatus() {
        runStatement(
                "Bổ sung trạng thái REJECTED cho auction_sessions.status",
                "ALTER TABLE auction_sessions MODIFY COLUMN status "
                        + "ENUM('PENDING','ACTIVE','ENDED','CANCELED','REJECTED') DEFAULT NULL"
        );
    }

    private void ensureItemHiddenColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) "
                            + "FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'items' "
                            + "AND COLUMN_NAME = 'hidden'",
                    Integer.class
            );

            if (count == null || count == 0) {
                runStatement(
                        "Bổ sung cột items.hidden để admin ẩn/hiện sản phẩm",
                        "ALTER TABLE items ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE"
                );
            }

            runUpdate(
                    "Chuẩn hóa sản phẩm chưa có trạng thái ẩn/hiện",
                    "UPDATE items SET hidden = FALSE WHERE hidden IS NULL"
            );
        } catch (Exception e) {
            logger.warn("Bổ sung cột items.hidden bị bỏ qua: {}", e.getMessage());
        }
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
            addCorrectBidSessionForeignKeyIfMissing();
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

    private void addCorrectBidSessionForeignKeyIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) "
                        + "FROM information_schema.KEY_COLUMN_USAGE "
                        + "WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = 'bids' "
                        + "AND COLUMN_NAME = 'session_id' "
                        + "AND REFERENCED_TABLE_NAME = 'auction_sessions'",
                Integer.class
        );

        if (count != null && count > 0) {
            logger.info("Khóa ngoại bids.session_id sang auction_sessions.id đã tồn tại");
            return;
        }

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
