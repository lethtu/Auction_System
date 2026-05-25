package com.auction.client.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;

public class UserTest {

    @BeforeEach
    public void setUp() {
        User.clearSession();
    }

    @Test
    public void testSetSession() {
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER", "http://avatar.url");
        
        Assertions.assertEquals(1, User.getId());
        Assertions.assertEquals("testuser", User.getUsername());
        Assertions.assertEquals("Nguyen Van A", User.getFullname());
        Assertions.assertEquals("test@example.com", User.getEmail());
        Assertions.assertEquals("2000-01-01", User.getDob());
        Assertions.assertEquals("Hanoi", User.getPlace_of_birth());
        Assertions.assertEquals("USER", User.getRole());
        Assertions.assertEquals("http://avatar.url", User.getAvatarUrl());
        Assertions.assertEquals(BigDecimal.ZERO, User.getBalance());
        Assertions.assertEquals(BigDecimal.ZERO, User.getFrozenBalance());
        Assertions.assertEquals(BigDecimal.ZERO, User.getAvailableBalance());
    }

    @Test
    public void testClearSession() {
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER", "url");
        User.setSessionToken("my-token");
        User.setPasswordSet(false);
        User.clearSession();
        
        Assertions.assertNull(User.getId());
        Assertions.assertNull(User.getUsername());
        Assertions.assertNull(User.getFullname());
        Assertions.assertNull(User.getAvatarUrl());
        Assertions.assertNull(User.getSessionToken());
        Assertions.assertTrue(User.isPasswordSet());
    }

    @Test
    public void testUpdateProfile() {
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER", "url");
        
        User.updateProfile("new_user", "Nguyen Van B", "new@example.com", "1999-12-31", "Saigon");
        Assertions.assertEquals("new_user", User.getUsername());
        Assertions.assertEquals("Nguyen Van B", User.getFullname());
        Assertions.assertEquals("new@example.com", User.getEmail());
        Assertions.assertEquals("1999-12-31", User.getDob());
        Assertions.assertEquals("Saigon", User.getPlace_of_birth());

        User.updateProfile("new_user2", "Nguyen Van C", "c@example.com", "1998-12-31", "Hue", new BigDecimal("1000"));
        Assertions.assertEquals("new_user2", User.getUsername());
        Assertions.assertEquals(new BigDecimal("1000"), User.getBalance());

        User.updateProfile("new_user3", "Nguyen Van D", "d@example.com", "1997-12-31", "Danang", new BigDecimal("2000"), "new_avatar");
        Assertions.assertEquals("new_avatar", User.getAvatarUrl());
        Assertions.assertEquals(new BigDecimal("2000"), User.getBalance());

        User.updateProfile("new_user4", "Nguyen Van E", "e@example.com", "1996-12-31", "Cantho", new BigDecimal("3000"), new BigDecimal("500"), "new_avatar2");
        Assertions.assertEquals("new_avatar2", User.getAvatarUrl());
        Assertions.assertEquals(new BigDecimal("3000"), User.getBalance());
        Assertions.assertEquals(new BigDecimal("500"), User.getFrozenBalance());
        Assertions.assertEquals(new BigDecimal("2500"), User.getAvailableBalance());
    }

    @Test
    public void testBalanceNullSafety() {
        User.setBalance(null);
        Assertions.assertEquals(BigDecimal.ZERO, User.getBalance());

        User.setFrozenBalance(null);
        Assertions.assertEquals(BigDecimal.ZERO, User.getFrozenBalance());
    }

    @Test
    public void testCachedAvatarImage() {
        User.clearCachedAvatarImage();
        Assertions.assertNull(User.getCachedAvatarImage());
        Assertions.assertNull(User.getCachedAvatarUrl());

        User.setCachedAvatarImage(null, "http://avatar.jpg");
        Assertions.assertNull(User.getCachedAvatarImage());
        Assertions.assertEquals("http://avatar.jpg", User.getCachedAvatarUrl());
    }
}
