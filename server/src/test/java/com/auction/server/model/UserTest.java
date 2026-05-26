package com.auction.server.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void settersTrimTextFieldsAndBlankValuesBecomeNull() {
        User user = new User();

        user.setUsername("  minh  ");
        user.setFullname("  Nguyen Le Quang Minh  ");
        user.setEmail("  minh@example.com  ");
        user.setDob("  2008-09-24  ");
        user.setPlaceOfBirth("  Ha Noi  ");

        assertEquals("minh", user.getUsername());
        assertEquals("Nguyen Le Quang Minh", user.getFullname());
        assertEquals("minh@example.com", user.getEmail());
        assertEquals("2008-09-24", user.getDob());
        assertEquals("Ha Noi", user.getPlaceOfBirth());
        assertEquals("Ha Noi", user.getPlace_of_birth());

        user.setUsername("   ");
        user.setFullname("   ");
        user.setEmail("   ");
        user.setDob("   ");
        user.setPlace_of_birth("   ");

        assertNull(user.getUsername());
        assertNull(user.getFullname());
        assertNull(user.getEmail());
        assertNull(user.getDob());
        assertNull(user.getPlaceOfBirth());
    }

    @Test
    void balanceFrozenBalanceAndAvailableBalanceAreNullSafe() {
        User user = new User();

        assertEquals(BigDecimal.ZERO, user.getBalance());
        assertEquals(BigDecimal.ZERO, user.getFrozenBalance());
        assertEquals(BigDecimal.ZERO, user.getAvailableBalance());

        user.setBalance(new BigDecimal("100.50"));
        user.setFrozenBalance(new BigDecimal("30.25"));

        assertEquals(new BigDecimal("100.50"), user.getBalance());
        assertEquals(new BigDecimal("30.25"), user.getFrozenBalance());
        assertEquals(new BigDecimal("70.25"), user.getAvailableBalance());

        user.setBalance(null);
        user.setFrozenBalance(null);

        assertEquals(BigDecimal.ZERO, user.getBalance());
        assertEquals(BigDecimal.ZERO, user.getFrozenBalance());
    }

    @Test
    void accountTypeDefaultsAndNormalizesRole() {
        User user = new User();

        assertEquals("user", user.getAccountType());
        assertEquals("user", user.getRole());

        user.setAccountType("  ADMIN  ");

        assertEquals("admin", user.getAccountType());
        assertEquals("admin", user.getRole());

        user.setAccountType("   ");

        assertEquals("user", user.getAccountType());
    }

    @Test
    void passwordSetNullIsTreatedAsTrue() {
        User user = new User();

        assertTrue(user.getPasswordSet());

        user.setPasswordSet(Boolean.FALSE);
        assertFalse(user.getPasswordSet());

        user.setPasswordSet(null);
        assertTrue(user.getPasswordSet());
    }

    @Test
    void miscFieldsAndToStringAreStable() {
        User user = new User();

        user.setId(7);
        user.setPassword("secret");
        user.setAvatarUrl("https://cdn.example/avatar.png");
        user.setSessionToken("token-abc");
        user.setBanned(true);
        user.setAccountType("seller");

        assertEquals(7, user.getId());
        assertEquals("secret", user.getPassword());
        assertEquals("https://cdn.example/avatar.png", user.getAvatarUrl());
        assertEquals("token-abc", user.getSessionToken());
        assertTrue(user.isBanned());
        assertTrue(user.toString().contains("role='seller'"));
        assertTrue(user.toString().contains("banned=true"));
    }
}
