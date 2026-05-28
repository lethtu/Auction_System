package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.model.Bidder;
import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.util.EmailTemplateBuilder;
import com.auction.server.util.PasswordUtil;
import com.auction.server.util.SessionManager;
import com.auction.server.view.EmailServer;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthController.class);

    private static final int OK_STATUS = 200;
    private static final int BAD_REQUEST_STATUS = 400;
    private static final int FORBIDDEN_STATUS = 403;
    private static final int SERVER_ERROR_STATUS = 500;

    private static final String MOCK_CLIENT_ID = "mock";
    private static final String DEFAULT_MOCK_EMAIL = "mockuser@gmail.com";
    private static final String DEFAULT_MOCK_NAME = "Mock Google User";
    private static final String EMPTY_RESPONSE_DATA = "";

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String PROFILE_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_AUTH_ERROR_MESSAGE = "Google Authentication error";
    private static final String LOGIN_SUCCESS_MESSAGE = "Login successful";
    private static final String ACCOUNT_BANNED_MESSAGE = "Account has been banned";
    private static final DateTimeFormatter EMAIL_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Autowired
    private HandleLoginSignup loginSignup;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private EmailServer emailServer;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GetMapping("/google/config")
    public ApiResponse<?> getGoogleConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("clientId", clientId);
        config.put("mock", isMockGoogleEnabled());
        return new ApiResponse<>(OK_STATUS, "Config retrieved", config);
    }

    @PostMapping("/google")
    public ApiResponse<?> googleLogin(@RequestBody Map<String, String> payload) {
        GoogleProfile profile;

        if (isMockGoogleEnabled()) {
            profile = buildMockProfile(payload);
            logger.info("Processing Mock Google Login for email: {}", profile.email);
        } else if (!hasText(payload.get("code")) || !hasText(payload.get("redirectUri"))) {
            return new ApiResponse<>(
                    BAD_REQUEST_STATUS,
                    "Missing authorization code or redirect URI",
                    EMPTY_RESPONSE_DATA);
        } else {
            try {
                profile = fetchGoogleProfile(payload.get("code"), payload.get("redirectUri"));
            } catch (GoogleAuthClientException e) {
                return new ApiResponse<>(e.status, e.getMessage(), EMPTY_RESPONSE_DATA);
            } catch (Exception e) {
                logger.error("Exception during Google Authentication: {}", e.getMessage(), e);
                return new ApiResponse<>(
                        SERVER_ERROR_STATUS,
                        GOOGLE_AUTH_ERROR_MESSAGE,
                        EMPTY_RESPONSE_DATA);
            }
        }

        UserRegistration registration = findOrCreateGoogleUser(profile);
        User user = registration.user;

        if (user.isBanned()) {
            logger.warn("Google User {} is banned", user.getUsername());
            return new ApiResponse<>(FORBIDDEN_STATUS, ACCOUNT_BANNED_MESSAGE, EMPTY_RESPONSE_DATA);
        }

        String token = sessionManager.createSession(user);
        user.setSessionToken(token);
        logger.info("Google User {} logged in successfully", user.getUsername());

        if (registration.newRegistration) {
            sendRegistrationEmailSafely(user);
        }

        return new ApiResponse<>(OK_STATUS, LOGIN_SUCCESS_MESSAGE, user);
    }

    private boolean isMockGoogleEnabled() {
        return clientId == null || clientId.isBlank() || MOCK_CLIENT_ID.equalsIgnoreCase(clientId);
    }

    private GoogleProfile buildMockProfile(Map<String, String> payload) {
        String email = payload.get("email");
        String name = payload.get("name");

        if (!hasText(email)) {
            email = DEFAULT_MOCK_EMAIL;
        }
        if (!hasText(name)) {
            name = DEFAULT_MOCK_NAME;
        }

        return new GoogleProfile(email, name);
    }

    private GoogleProfile fetchGoogleProfile(String code, String redirectUri) throws Exception {
        String accessToken = exchangeAuthorizationCode(code, redirectUri);

        HttpRequest profileRequest = HttpRequest.newBuilder()
                .uri(URI.create(PROFILE_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> profileResponse = httpClient.send(
                profileRequest,
                HttpResponse.BodyHandlers.ofString());
        if (profileResponse.statusCode() != OK_STATUS) {
            logger.error(
                    "Failed to fetch profile: status={}, body={}",
                    profileResponse.statusCode(),
                    profileResponse.body());
            throw new GoogleAuthClientException(
                    BAD_REQUEST_STATUS,
                    "Failed to retrieve user profile from Google");
        }

        JSONObject profileJson = new JSONObject(profileResponse.body());
        String email = profileJson.optString("email");
        String name = profileJson.optString("name", email);

        if (!hasText(email)) {
            throw new GoogleAuthClientException(BAD_REQUEST_STATUS, "Email not provided by Google");
        }

        logger.info("Google Authentication successful for email: {}", email);
        return new GoogleProfile(email, name);
    }

    private String exchangeAuthorizationCode(String code, String redirectUri) throws Exception {
        String requestBody = "code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != OK_STATUS) {
            logger.error("Failed to exchange code: status={}, body={}", response.statusCode(), response.body());
            throw new GoogleAuthClientException(
                    BAD_REQUEST_STATUS,
                    "Failed to exchange Google authorization code");
        }

        JSONObject tokenResponse = new JSONObject(response.body());
        String accessToken = tokenResponse.optString("access_token");
        if (!hasText(accessToken)) {
            throw new GoogleAuthClientException(
                    BAD_REQUEST_STATUS,
                    "Failed to retrieve access token from Google");
        }

        return accessToken;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private UserRegistration findOrCreateGoogleUser(GoogleProfile profile) {
        Optional<User> userOpt = loginSignup.findByUsernameOrEmail(profile.email);
        if (userOpt.isPresent()) {
            return new UserRegistration(userOpt.get(), false);
        }

        Bidder bidder = new Bidder();
        bidder.setUsername(generateUniqueUsername(profile.email));
        bidder.setFullname(profile.name);
        bidder.setEmail(profile.email);
        bidder.setPassword(PasswordUtil.hashPassword(UUID.randomUUID().toString()));
        bidder.setBalance(BigDecimal.ZERO);
        bidder.setPasswordSet(false);

        User user = loginSignup.save(bidder);
        logger.info("Auto-registered new Google user as BIDDER: username={}", bidder.getUsername());
        return new UserRegistration(user, true);
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int suffix = 1;

        while (loginSignup.findByUsernameOrEmail(username).isPresent()) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }

    private void sendRegistrationEmailSafely(User user) {
        try {
            String timeStr = LocalDateTime.now().format(EMAIL_TIME_FORMATTER);
            String emailBody = EmailTemplateBuilder.buildGoogleRegisterEmail(
                    user.getFullname(),
                    user.getEmail(),
                    timeStr);
            emailServer.SendEmail(
                    user.getEmail(),
                    "Welcome to BidPop - Registration Successful",
                    emailBody);
        } catch (Exception e) {
            logger.error("Failed to send Google registration email notification", e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static class GoogleProfile {
        private final String email;
        private final String name;

        private GoogleProfile(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }

    private static class UserRegistration {
        private final User user;
        private final boolean newRegistration;

        private UserRegistration(User user, boolean newRegistration) {
            this.user = user;
            this.newRegistration = newRegistration;
        }
    }

    private static class GoogleAuthClientException extends Exception {
        private final int status;

        private GoogleAuthClientException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
