package com.auction.server.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    public void testHashPassword() {
        assertNull(PasswordUtil.hashPassword(null));
        String pass = "myPassword123";
        String hashed = PasswordUtil.hashPassword(pass);
        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2a$") || hashed.startsWith("$2b$") || hashed.startsWith("$2y$"));
    }

    @Test
    public void testCheckPassword() {
        assertFalse(PasswordUtil.checkPassword(null, "someHash"));
        assertFalse(PasswordUtil.checkPassword("plain", null));
        assertFalse(PasswordUtil.checkPassword(null, null));

        String plain = "mySecret";
        String hashed = PasswordUtil.hashPassword(plain);

        assertTrue(PasswordUtil.checkPassword(plain, hashed));
        assertFalse(PasswordUtil.checkPassword("wrong", hashed));

        assertTrue(PasswordUtil.checkPassword("plainPass", "plainPass"));
        assertFalse(PasswordUtil.checkPassword("plainPass", "otherPass"));

        assertFalse(PasswordUtil.checkPassword("plain", "$2a$12$invalidhashcharacterstocauseexception"));
    }

    @Test
    void checkPassword_invalidBcryptFormatFallsBackToPlainComparison() {
        String invalidBcryptLikePlainText = "$2a$broken";

        assertTrue(PasswordUtil.checkPassword(invalidBcryptLikePlainText, invalidBcryptLikePlainText));
        assertFalse(PasswordUtil.checkPassword("different", invalidBcryptLikePlainText));
    }

    @Test
    void checkPassword_supportsBcrypt2bPrefix() {
        String plain = "prefixCheck";
        String hashed = PasswordUtil.hashPassword(plain);
        String normalized = hashed.replaceFirst("^\\$2a\\$", "\\$2b\\$");

        assertTrue(PasswordUtil.checkPassword(plain, normalized));
    }

    @Test
    public void testIsPlaintext() {
        assertFalse(PasswordUtil.isPlaintext(null));
        assertTrue(PasswordUtil.isPlaintext("plainTextPwd"));
        assertTrue(PasswordUtil.isPlaintext(""));
        assertTrue(PasswordUtil.isPlaintext(" $2a$12$notReallyBecauseLeadingSpace"));
        assertFalse(PasswordUtil.isPlaintext("$2a$12$somevalidlookinghashhere"));
        assertFalse(PasswordUtil.isPlaintext("$2b$12$somevalidlookinghashhere"));
        assertFalse(PasswordUtil.isPlaintext("$2y$12$somevalidlookinghashhere"));
    }
}
