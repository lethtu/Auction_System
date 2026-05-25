package com.auction.server.util;

import com.auction.server.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    public void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    public void testCreateSession_NullUser() {
        assertThrows(IllegalArgumentException.class, () -> sessionManager.createSession(null));
        User emptyUser = new User();
        assertThrows(IllegalArgumentException.class, () -> sessionManager.createSession(emptyUser));
    }

    @Test
    public void testCreateSession_Success() {
        User user = new User();
        user.setId(100);
        user.setUsername("testuser");
        user.setAccountType("BIDDER");

        String token = sessionManager.createSession(user);
        assertNotNull(token);

        SessionManager.SessionUser sessionUser = sessionManager.getSession(token);
        assertNotNull(sessionUser);
        assertEquals(100, sessionUser.getUserId());
        assertEquals("testuser", sessionUser.getUsername());
        assertEquals("bidder", sessionUser.getRole()); // role is lowercased
    }

    @Test
    public void testGetSession_NullOrMissingToken() {
        assertNull(sessionManager.getSession(null));
        assertNull(sessionManager.getSession("nonexistent-token"));
    }

    @Test
    public void testRemoveSession() {
        User user = new User();
        user.setId(101);
        user.setUsername("user2");
        user.setAccountType("SELLER");

        String token = sessionManager.createSession(user);
        assertNotNull(sessionManager.getSession(token));

        sessionManager.removeSession(token);
        assertNull(sessionManager.getSession(token));

        // removing null should not throw exception
        assertDoesNotThrow(() -> sessionManager.removeSession(null));
    }

    @Test
    public void testUpdateRoleByUserId() {
        User user1 = new User();
        user1.setId(102);
        user1.setUsername("user3");
        user1.setAccountType("BIDDER");

        User user2 = new User();
        user2.setId(103);
        user2.setUsername("user4");
        user2.setAccountType("SELLER");

        String token1 = sessionManager.createSession(user1);
        String token2 = sessionManager.createSession(user2);

        sessionManager.updateRoleByUserId(102, "admin");
        sessionManager.updateRoleByUserId(null, "someRole");
        sessionManager.updateRoleByUserId(102, null);

        assertEquals("admin", sessionManager.getSession(token1).getRole());
        assertEquals("seller", sessionManager.getSession(token2).getRole());
    }

    @Test
    public void testClearAll() {
        User user = new User();
        user.setId(104);
        user.setUsername("user5");
        String token = sessionManager.createSession(user);

        assertNotNull(sessionManager.getSession(token));
        sessionManager.clearAll();
        assertNull(sessionManager.getSession(token));
    }
}
