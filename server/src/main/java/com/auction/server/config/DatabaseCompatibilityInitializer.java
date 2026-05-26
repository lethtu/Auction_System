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
                    "Allow nullable users.dtype column for legacy data",
                    "ALTER TABLE users MODIFY COLUMN dtype VARCHAR(31) NULL DEFAULT NULL"
            );
            runUpdate(
                    "Sync legacy dtype from role",
                    "UPDATE users SET dtype = role WHERE dtype IS NULL AND role IS NOT NULL"
            );
        } catch (Exception e) {
            logger.warn("Fixing legacy users.dtype column skipped: {}", e.getMessage());
        }
    }

    private void normalizeRoleValues() {
        runUpdate(
                "Normalize bidder role",
                "UPDATE users SET role = 'bidder' "
                        + "WHERE id >= 0 AND role IS NOT NULL AND LOWER(role) = 'bidder'"
        );
        runUpdate(
                "Normalize seller role",
                "UPDATE users SET role = 'seller' "
                        + "WHERE id >= 0 AND role IS NOT NULL AND LOWER(role) = 'seller'"
        );
        runUpdate(
                "Normalize admin role",
                "UPDATE users SET role = 'admin' "
                        + "WHERE id >= 0 AND role IS NOT NULL AND LOWER(role) = 'admin'"
        );
        runUpdate(
                "Normalize user role",
                "UPDATE users SET role = 'user' "
                        + "WHERE id >= 0 AND (role IS NULL OR TRIM(role) = '' OR LOWER(role) = 'user')"
        );
    }

    private void allowRejectedAuctionStatus() {
        runStatement(
                "Update auction_sessions.status column enum values",
                "ALTER TABLE auction_sessions MODIFY COLUMN status "
                        + "ENUM('ACTIVE','ENDED','PAID','CANCELED','COMING','DRAFT') NOT NULL"
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
                        "Add items.hidden column for admin to show/hide products",
                        "ALTER TABLE items ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE"
                );
            }

            runUpdate(
                    "Normalize products without hidden status",
                    "UPDATE items SET hidden = FALSE WHERE hidden IS NULL"
            );
        } catch (Exception e) {
            logger.warn("Adding items.hidden column skipped: {}", e.getMessage());
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
            logger.warn("Fixing bids.session_id foreign key skipped: {}", e.getMessage());
        }
    }

    private void dropForeignKey(String foreignKey) {
        String safeForeignKey = foreignKey.replace("`", "``");
        runStatement(
                "Drop old bids.session_id foreign key: " + foreignKey,
                "ALTER TABLE bids DROP FOREIGN KEY `" + safeForeignKey + "`"
        );
    }

    private void removeOrphanBids() {
        runUpdate(
                "Remove orphan bids with no valid auction session",
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
            logger.info("Foreign key bids.session_id to auction_sessions.id already exists");
            return;
        }

        runStatement(
                "Add foreign key bids.session_id to auction_sessions.id",
                "ALTER TABLE bids ADD CONSTRAINT FK_bids_session_auction_sessions "
                        + "FOREIGN KEY (session_id) REFERENCES auction_sessions(id) "
                        + "ON DELETE CASCADE ON UPDATE CASCADE"
        );
    }

    private void runUpdate(String action, String sql) {
        try {
            int affectedRows = jdbcTemplate.update(sql);
            logger.info("{} completed, rows affected: {}", action, affectedRows);
        } catch (Exception e) {
            logger.warn("{} skipped: {}", action, e.getMessage());
        }
    }

    private void runStatement(String action, String sql) {
        try {
            jdbcTemplate.execute(sql);
            logger.info("{} completed", action);
        } catch (Exception e) {
            logger.warn("{} skipped: {}", action, e.getMessage());
        }
    }
}
