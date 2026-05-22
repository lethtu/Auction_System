package com.auction.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionManager sessionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // CORS preflight requests should always pass
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("X-Auth-Token");
        SessionManager.SessionUser sessionUser = sessionManager.getSession(token);

        if (sessionUser == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid or missing session token.");
            return false;
        }

        // 1. Admin endpoints role enforcement
        if (path.startsWith("/api/admin/")) {
            if (!"admin".equalsIgnoreCase(sessionUser.getRole())) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Admin access required.");
                return false;
            }
        }

        // 2. Seller endpoints role enforcement
        if (path.startsWith("/api/seller/")) {
            if (!"seller".equalsIgnoreCase(sessionUser.getRole()) && !"admin".equalsIgnoreCase(sessionUser.getRole())) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Seller access required.");
                return false;
            }
        }

        // 3. Bidder endpoints self-resource protection
        if (path.startsWith("/api/bidder/")) {
            if (!path.equals("/api/bidder/active-sessions")) {
                String bidderIdParam = request.getParameter("bidderId");
                String userIdParam = request.getParameter("userId");
                Integer targetId = null;
                try {
                    if (bidderIdParam != null) {
                        targetId = Integer.parseInt(bidderIdParam);
                    } else if (userIdParam != null) {
                        targetId = Integer.parseInt(userIdParam);
                    }
                } catch (NumberFormatException ignored) {}

                if (targetId != null) {
                    if (!targetId.equals(sessionUser.getUserId()) && !"admin".equalsIgnoreCase(sessionUser.getRole())) {
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: You cannot access or modify other users' data.");
                        return false;
                    }
                }
            }
        }

        // 3. User-specific endpoints (self-resource protection)
        // E.g., PUT /api/users/{id}/profile, POST /api/users/{id}/avatar
        if (path.startsWith("/api/users/")) {
            String[] parts = path.split("/");
            if (parts.length > 3) {
                try {
                    Integer targetUserId = Integer.parseInt(parts[3]);
                    // User can only modify their own resource, unless they are an admin
                    if (!targetUserId.equals(sessionUser.getUserId()) && !"admin".equalsIgnoreCase(sessionUser.getRole())) {
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: You cannot modify other users' profiles.");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // Path isn't /api/users/{id}/... (e.g. /api/users or other format)
                }
            }
        }

        // Attach session user info to request attributes for downstream controller consumption if needed
        request.setAttribute("sessionUser", sessionUser);
        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws Exception {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorBody = new java.util.HashMap<>();
        errorBody.put("status", statusCode);
        errorBody.put("message", message);
        errorBody.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
