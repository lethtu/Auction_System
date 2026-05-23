package com.auction.server.util;

import com.auction.server.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    public static class SessionUser {
        private final Integer userId;
        private final String username;
        private final String role;

        public SessionUser(Integer userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }

    private final Map<String, SessionUser> activeSessions = new ConcurrentHashMap<>();

    /**
     * Creates a new session for the logged-in user and returns the generated token.
     */
    public String createSession(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID must not be null");
        }
        String token = UUID.randomUUID().toString();
        String role = user.getClass().getSimpleName().toLowerCase();
        // Fallback or custom handling if needed, but simpleClassName is usually bidder, seller, admin
        if (user.getRole() != null) {
            role = user.getRole().toLowerCase();
        }
        
        SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername(), role);
        activeSessions.put(token, sessionUser);
        return token;
    }

    /**
     * Gets the session associated with the token.
     */
    public SessionUser getSession(String token) {
        if (token == null) {
            return null;
        }
        return activeSessions.get(token);
    }

    /**
     * Removes the session associated with the token.
     */
    /**
     * Updates the role stored in an existing session after an account role change.
     */
    public void updateSessionRole(String token, String role) {
        if (token == null || role == null || role.isBlank()) {
            return;
        }
        SessionUser current = activeSessions.get(token);
        if (current == null) {
            return;
        }
        activeSessions.put(token, new SessionUser(current.getUserId(), current.getUsername(), role.toLowerCase()));
    }
    public void removeSession(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    /**
     * Clears all sessions.
     */
    public void clearAll() {
        activeSessions.clear();
    }
}
