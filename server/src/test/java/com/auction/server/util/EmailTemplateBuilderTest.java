package com.auction.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateBuilderTest {

    @Test
    void constructor_canBeCreated() {
        assertDoesNotThrow(EmailTemplateBuilder::new);
    }

    @Test
    void buildWelcomeEmail_containsExpectedUserAndBrandContent() {
        String fullname = "John Doe";
        String username = "johndoe";

        String html = EmailTemplateBuilder.buildWelcomeEmail(fullname, username);

        assertNotNull(html);
        assertTrue(html.contains("Welcome to BidPop!"));
        assertTrue(html.contains("BIDPOP AUCTION"));
        assertTrue(html.contains("Account Registration Successful!"));
        assertTrue(html.contains(fullname));
        assertTrue(html.contains(username));
    }

    @Test
    void buildForgotPassEmail_containsOtpAndSecurityNotice() {
        String fullname = "Alice Smith";
        String otpCode = "123456";

        String html = EmailTemplateBuilder.buildForgotPassEmail(fullname, otpCode);

        assertNotNull(html);
        assertTrue(html.contains("Password Reset Verification Code"));
        assertTrue(html.contains("SECURITY NOTICE"));
        assertTrue(html.contains(fullname));
        assertTrue(html.contains(otpCode));
    }

    @Test
    void buildGoogleRegisterEmail_delegatesToWelcomeEmailUsingEmailAsUsername() {
        String fullname = "Bob Johnson";
        String email = "bob@gmail.com";
        String regTime = "2026-05-25";

        String html = EmailTemplateBuilder.buildGoogleRegisterEmail(fullname, email, regTime);

        assertNotNull(html);
        assertTrue(html.contains("Welcome to BidPop!"));
        assertTrue(html.contains(fullname));
        assertTrue(html.contains(email));
    }
}
