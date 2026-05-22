package com.auction.server.util;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    @Autowired
    private HandleLoginSignup userRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting check for legacy plaintext passwords to migrate to BCrypt...");
        List<User> users = userRepository.findAll();
        int migratedCount = 0;

        for (User user : users) {
            String currentPassword = user.getPassword();
            if (PasswordUtil.isPlaintext(currentPassword)) {
                logger.info("Migrating password for user: {}", user.getUsername());
                String hashedPassword = PasswordUtil.hashPassword(currentPassword);
                user.setPassword(hashedPassword);
                userRepository.save(user);
                migratedCount++;
            }
        }

        if (migratedCount > 0) {
            logger.info("Password migration finished! Successfully encrypted {} user(s) to BCrypt.", migratedCount);
        } else {
            logger.info("All user passwords in database are already encrypted using BCrypt.");
        }
    }
}
