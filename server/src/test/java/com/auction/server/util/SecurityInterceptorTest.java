package com.auction.server.util;

import com.auction.server.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SecurityInterceptorTest {

    private SecurityInterceptor interceptor;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new SecurityInterceptor();
        sessionManager = new SessionManager();

        Field field = SecurityInterceptor.class.getDeclaredField("sessionManager");
        field.setAccessible(true);
        field.set(interceptor, sessionManager);
    }

    @Test
    void optionsRequest_passesWithoutSessionToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void missingToken_returnsUnauthorizedJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized: Invalid or missing session token."));
    }

    @Test
    void adminEndpoint_blocksNonAdminUser() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/admin/users", tokenFor(10, "bidder"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Forbidden: Admin access required."));
    }

    @Test
    void sellerEndpoint_allowsSellerAndStoresSessionUser() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/seller/my-sessions", tokenFor(20, "seller"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Object sessionUser = request.getAttribute("sessionUser");
        assertInstanceOf(SessionManager.SessionUser.class, sessionUser);
        assertEquals(20, ((SessionManager.SessionUser) sessionUser).getUserId());
    }

    @Test
    void sellerEndpoint_blocksBidder() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/seller/my-sessions", tokenFor(21, "bidder"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Forbidden: Seller access required."));
    }

    @Test
    void bidderEndpoint_blocksAccessToOtherBidderId() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/bidder/my-bids", tokenFor(30, "bidder"));
        request.setParameter("bidderId", "31");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Forbidden: You cannot access or modify other users' data."));
    }

    @Test
    void bidderActiveSessions_allowsAuthenticatedUserWithoutSelfCheck() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/bidder/active-sessions", tokenFor(40, "bidder"));
        request.setParameter("bidderId", "999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNotNull(request.getAttribute("sessionUser"));
    }

    @Test
    void userEndpoint_blocksOtherUserProfileForNonAdmin() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("PUT", "/api/users/51/profile", tokenFor(50, "seller"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Forbidden: You cannot modify other users' profiles."));
    }

    @Test
    void userEndpoint_allowsAdminToAccessOtherProfile() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("PUT", "/api/users/61/profile", tokenFor(1, "admin"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNotNull(request.getAttribute("sessionUser"));
    }

    private MockHttpServletRequest authenticatedRequest(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("X-Auth-Token", token);
        return request;
    }

    private String tokenFor(Integer userId, String role) {
        User user = new User();
        user.setId(userId);
        user.setUsername(role + "_user");
        user.setAccountType(role);
        return sessionManager.createSession(user);
    }
}