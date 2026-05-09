package com.auction.client.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class UserTest {

    @BeforeEach
    public void setUp() {
        User.clearSession();
    }

    @Test
    public void testSetSession() {
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER");
        
        Assertions.assertEquals(1, User.getId());
        Assertions.assertEquals("testuser", User.getUsername());
        Assertions.assertEquals("Nguyen Van A", User.getFullname());
        Assertions.assertEquals("test@example.com", User.getEmail());
        Assertions.assertEquals("2000-01-01", User.getDob());
        Assertions.assertEquals("Hanoi", User.getPlace_of_birth());
        Assertions.assertEquals("USER", User.getRole());
    }

    @Test
    public void testClearSession() {
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER");
        User.clearSession();
        
        Assertions.assertNull(User.getId());
        Assertions.assertNull(User.getUsername());
        Assertions.assertNull(User.getFullname());
    }
}
