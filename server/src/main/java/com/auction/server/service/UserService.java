package com.auction.server.service;

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
            throw new IllegalArgumentException("userId không hợp lệ");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String username = normalizeRequired(request.get("username"), "Tên đăng nhập không được để trống");
        String fullname = normalizeRequired(request.get("fullname"), "Họ tên không được để trống");
        String email = normalizeRequired(request.get("email"), "Email không được để trống");
        String dob = normalizeOptional(request.get("dob"));
        String placeOfBirth = normalizeOptional(firstNonBlank(request.get("placeOfBirth"), request.get("place_of_birth")));

        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        userRepository.findByUsername(username)
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
                });

        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Email đã được sử dụng");
                });

        user.setUsername(username);
        user.setFullname(fullname);
        user.setEmail(email);
        user.setDob(dob);
        user.setPlaceOfBirth(placeOfBirth);

        return userRepository.save(user);
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(errorMessage);
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

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalizeOptional(first);
        return normalizedFirst != null ? normalizedFirst : normalizeOptional(second);
    }
}
