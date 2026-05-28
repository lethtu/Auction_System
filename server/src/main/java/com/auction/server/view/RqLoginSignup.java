package com.auction.server.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.exception.ValidationException;
import com.auction.server.model.Bidder;
import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import com.auction.server.util.PasswordUtil;

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

    public Optional<User> signup(User newUser) {
        if (!loginSignup.existsByUsernameOrEmail(newUser.getUsername(), newUser.getEmail())) {
            logger.info("Adding user: {}", newUser.getUsername());
            if (newUser.getPassword() == null) {
                logger.info("Error: password for user {} is null", newUser.getUsername());
                throw new ValidationException("Password is required.");
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
            bidder.setBalance(newUser.getBalance() != null ? newUser.getBalance() : java.math.BigDecimal.ZERO);

            User savedUser = loginSignup.save(bidder);
            logger.info("Successfully added user: {} to DB with role BIDDER", newUser.getUsername());
            return Optional.of(savedUser);
        }
        logger.info("Username: {} or Email: {} already exists in DB", newUser.getUsername(), newUser.getEmail());
        return Optional.empty();
    }
}

