package com.auction.server.controller;

import com.auction.server.model.Bidder;
import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.dto.ApiResponse;
import com.auction.server.util.SessionManager;
import com.auction.server.util.PasswordUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthController.class);

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Autowired
    private HandleLoginSignup loginSignup;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private com.auction.server.view.EmailServer emailServer;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GetMapping("/google/config")
    public ApiResponse<?> getGoogleConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("clientId", clientId);
        config.put("mock", "mock".equalsIgnoreCase(clientId) || clientId == null || clientId.isBlank());
        return new ApiResponse<>(200, "Config retrieved", config);
    }

    @PostMapping("/google")
    public ApiResponse<?> googleLogin(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String redirectUri = payload.get("redirectUri");

        boolean isMock = "mock".equalsIgnoreCase(clientId) || clientId == null || clientId.isBlank();

        String email = null;
        String name = null;

        if (isMock) {
            email = payload.get("email");
            name = payload.get("name");
            if (email == null || email.isBlank()) {
                email = "mockuser@gmail.com";
            }
            if (name == null || name.isBlank()) {
                name = "Mock Google User";
            }
            logger.info("Processing Mock Google Login for email: {}", email);
        } else {
            if (code == null || code.isBlank() || redirectUri == null || redirectUri.isBlank()) {
                return new ApiResponse<>(400, "Missing authorization code or redirect URI", "");
            }

            try {
                // Exchange Auth Code for Access Token
                String requestBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&grant_type=authorization_code";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    logger.error("Failed to exchange code: status={}, body={}", response.statusCode(), response.body());
                    return new ApiResponse<>(400, "Failed to exchange Google authorization code", "");
                }

                JSONObject tokenResponse = new JSONObject(response.body());
                String accessToken = tokenResponse.optString("access_token");
                if (accessToken == null || accessToken.isBlank()) {
                    return new ApiResponse<>(400, "Failed to retrieve access token from Google", "");
                }

                // Fetch User Profile using Access Token
                HttpRequest profileRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();

                HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString());
                if (profileResponse.statusCode() != 200) {
                    logger.error("Failed to fetch profile: status={}, body={}", profileResponse.statusCode(), profileResponse.body());
                    return new ApiResponse<>(400, "Failed to retrieve user profile from Google", "");
                }

                JSONObject profileJson = new JSONObject(profileResponse.body());
                email = profileJson.optString("email");
                name = profileJson.optString("name", email);

                if (email == null || email.isBlank()) {
                    return new ApiResponse<>(400, "Email not provided by Google", "");
                }

                logger.info("Google Authentication successful for email: {}", email);

            } catch (Exception e) {
                logger.error("Exception during Google Authentication: {}", e.getMessage(), e);
                return new ApiResponse<>(500, "Google Authentication error: " + e.getMessage(), "");
            }
        }

        // Check if user exists, else auto-register as Bidder
        Optional<User> userOpt = loginSignup.findByUsernameOrEmail(email);
        User user;
        boolean isNewRegistration = false;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            int suffix = 1;
            while (loginSignup.findByUsernameOrEmail(username).isPresent()) {
                username = baseUsername + suffix;
                suffix++;
            }

            Bidder bidder = new Bidder();
            bidder.setUsername(username);
            bidder.setFullname(name);
            bidder.setEmail(email);
            bidder.setPassword(PasswordUtil.hashPassword(UUID.randomUUID().toString()));
            bidder.setBalance(java.math.BigDecimal.ZERO);
            bidder.setPasswordSet(false);

            user = loginSignup.save(bidder);
            isNewRegistration = true;
            logger.info("Auto-registered new Google user as BIDDER: username={}", username);
        }

        if (user.isBanned()) {
            logger.warn("Google User {} is banned", user.getUsername());
            return new ApiResponse<>(403, "Account has been banned", "");
        }

        // Generate session token
        String token = sessionManager.createSession(user);
        user.setSessionToken(token);
        logger.info("Google User {} logged in successfully", user.getUsername());

        // Send registration notification email
        if (isNewRegistration) {
            try {
                String timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                String emailBody = com.auction.server.util.EmailTemplateBuilder.buildGoogleRegisterEmail(user.getFullname(), user.getEmail(), timeStr);
                emailServer.SendEmail(user.getEmail(), "Chào Mừng Bạn Đến Với BidPop - Đăng Ký Thành Công", emailBody);
            } catch (Exception e) {
                logger.error("Failed to send Google registration email notification", e);
            }
        }

        return new ApiResponse<>(200, "Login successful", user);
    }
}
