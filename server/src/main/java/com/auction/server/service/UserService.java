package com.auction.server.service;

import com.auction.server.exception.BusinessException;
import com.auction.server.exception.ResourceNotFoundException;
import com.auction.server.exception.ValidationException;
import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

    public User updateProfile(Integer id, Map<String, String> request) {
        if (id == null || id <= 0) {
            throw new ValidationException("Invalid userId");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String username = normalizeRequired(request.get("username"), "Username cannot be empty");
        String fullname = normalizeRequired(request.get("fullname"), "Full name cannot be empty");
        String email = normalizeRequired(request.get("email"), "Email cannot be empty");
        String dob = normalizeOptional(request.get("dob"));
        String placeOfBirth = normalizeOptional(firstNonBlank(request.get("placeOfBirth"), request.get("place_of_birth")));

        if (!email.contains("@")) {
            throw new ValidationException("Invalid email");
        }

        userRepository.findByUsername(username)
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new BusinessException("Username is already taken");
                });

        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new BusinessException("Email is already taken");
                });

        user.setUsername(username);
        user.setFullname(fullname);
        user.setEmail(email);
        user.setDob(dob);
        user.setPlaceOfBirth(placeOfBirth);

        return userRepository.save(user);
    }

    public User updateAvatarUrl(Integer userId, String avatarUrl) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Invalid userId");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ValidationException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public User setPassword(Integer id, String password) {
        if (id == null || id <= 0) {
            throw new ValidationException("Invalid userId");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPasswordSet() != null && user.getPasswordSet()) {
            throw new BusinessException("Password has already been set for this account.");
        }

        if (password == null || password.trim().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }

        user.setPassword(com.auction.server.util.PasswordUtil.hashPassword(password));
        user.setPasswordSet(true);
        return userRepository.save(user);
    }

    public User changePassword(Integer id, String oldPassword, String newPassword) {
        if (id == null || id <= 0) {
            throw new ValidationException("Invalid userId");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPasswordSet() == null || !user.getPasswordSet() || user.getPassword() == null) {
            throw new BusinessException("Please set a password for this account first.");
        }

        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new ValidationException("Current password cannot be empty.");
        }

        if (!com.auction.server.util.PasswordUtil.checkPassword(oldPassword, user.getPassword())) {
            throw new BusinessException("Incorrect current password.");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long.");
        }

        user.setPassword(com.auction.server.util.PasswordUtil.hashPassword(newPassword));
        return userRepository.save(user);
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalizeOptional(first);
        return normalizedFirst != null ? normalizedFirst : normalizeOptional(second);
    }
}
