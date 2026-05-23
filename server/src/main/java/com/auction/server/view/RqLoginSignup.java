package com.auction.server.view;

import com.auction.server.model.Bidder;
import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class RqLoginSignup {
    private static final Logger logger = LoggerFactory.getLogger(RqLoginSignup.class);

    @Autowired
    private HandleLoginSignup loginSignup;

    public Optional<User> login(String username, String pass) {
        Optional<User> userOpt = loginSignup.findByUsernameOrEmail(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtil.checkPassword(pass, user.getPassword())) {
                // If the password is still stored as plaintext, upgrade it to hashed
                if (PasswordUtil.isPlaintext(user.getPassword())) {
                    logger.info("Migrating plaintext password to BCrypt for user: {}", user.getUsername());
                    user.setPassword(PasswordUtil.hashPassword(pass));
                    loginSignup.save(user);
                }
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public boolean signup(User newUser) {
        if (!loginSignup.existsByUsernameOrEmail(newUser.getUsername(), newUser.getEmail())) {
            logger.info("Adding user: {}", newUser.getUsername());
            if (newUser.getPassword() == null) {
                logger.info("Error: password for user {} is null", newUser.getUsername());
                throw new RuntimeException("Error: Password sent is null!");
            }

            // Always create Bidder on signup so Hibernate writes "BIDDER" discriminator to DB
            Bidder bidder = new Bidder();
            bidder.setUsername(newUser.getUsername());
            // Hash the password before saving
            bidder.setPassword(PasswordUtil.hashPassword(newUser.getPassword()));
            bidder.setFullname(newUser.getFullname());
            bidder.setEmail(newUser.getEmail());
            bidder.setDob(newUser.getDob());
            bidder.setPlace_of_birth(newUser.getPlace_of_birth());
            bidder.setBalance(newUser.getBalance() != null ? newUser.getBalance() : BigDecimal.ZERO);
            bidder.setPasswordSet(true);

            loginSignup.save(bidder);
            logger.info("Successfully added user: {} to DB with role BIDDER", newUser.getUsername());
            return false;
        }
        logger.info("Username: {} or Email: {} already exists in DB", newUser.getUsername(), newUser.getEmail());
        return true;
    }

    public User findOrCreateGoogleUser(String email, String fullname, String avatarUrl) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> existingUser = loginSignup.findByEmail(normalizedEmail);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            boolean changed = false;

            if ((user.getFullname() == null || user.getFullname().isBlank()) && fullname != null && !fullname.isBlank()) {
                user.setFullname(fullname);
                changed = true;
            }

            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatarUrl(avatarUrl);
                changed = true;
            }

            return changed ? loginSignup.save(user) : user;
        }

        Bidder bidder = new Bidder();
        bidder.setEmail(normalizedEmail);
        bidder.setUsername(buildUniqueGoogleUsername(normalizedEmail));
        bidder.setFullname((fullname == null || fullname.isBlank()) ? normalizedEmail : fullname.trim());
        bidder.setPassword(PasswordUtil.hashPassword(UUID.randomUUID().toString()));
        bidder.setBalance(BigDecimal.ZERO);
        bidder.setAvatarUrl(avatarUrl);
        bidder.setPasswordSet(false);

        User savedUser = loginSignup.save(bidder);
        logger.info("Created Google bidder account for {}", normalizedEmail);
        return savedUser;
    }

    public boolean googleUserExistsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return loginSignup.findByEmail(email.trim().toLowerCase()).isPresent();
    }
    private String buildUniqueGoogleUsername(String email) {
        String base = email.substring(0, email.indexOf('@'))
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_");

        if (base.isBlank()) {
            base = "google_user";
        }

        String candidate = base;
        int suffix = 1;
        while (loginSignup.existsByUsername(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }
}
