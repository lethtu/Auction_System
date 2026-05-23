package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.model.User;
import com.auction.server.util.EmailTemplateBuilder;
import com.auction.server.util.SessionManager;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqLoginSignup;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthLoginSignup {
    private static final Logger logger = LoggerFactory.getLogger(AuthLoginSignup.class);

    @Autowired
    private RqLoginSignup rq;

    @Autowired
    private EmailServer emailServer;

    @Autowired
    private SessionManager sessionManager;

    @Value("${google.oauth.mock:true}")
    private boolean googleMock;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostMapping("/login")
    public ApiResponse<?> Login(@RequestBody Map<String, String> requests) {
        String username = requests.get("username");
        String password = requests.get("password");
        Optional<User> res = rq.login(username, password);
        if (res.isPresent()) {
            User user = res.get();
            if (user.isBanned()) {
                logger.warn("User {} account is banned", username);
                return new ApiResponse<String>(403, "Account has been banned", "");
            }
            String token = sessionManager.createSession(user);
            user.setSessionToken(token);
            logger.info("User {} logged in successfully", username);
            return new ApiResponse<User>(200, "Login successful", user);
        }
        else {
            logger.error("User {} login failed", username);
            return new ApiResponse<String>(400, "Login failed", "");
        }
    }

    @PostMapping("/signup")
    public ApiResponse<?> Signup(@RequestBody User newUser) {
        logger.info("New user registration info: {}", newUser);
        logger.info("Email: {}, Fullname: {}, Password: {}", newUser.getEmail(), newUser.getFullname(), newUser.getPassword());
        boolean check = rq.signup(newUser);
        if (!check) {
            String body = EmailTemplateBuilder.buildWelcomeEmail(newUser.getFullname(), newUser.getUsername());
            emailServer.SendEmail(newUser.getEmail(), "Account Registration Successful", body);
            logger.info("User {} registered successfully", newUser.getUsername());
            return new ApiResponse<User>(200, "Registration successful", newUser);
        }
        else {
            logger.error("Registration error, username: {} or email: {} already exists", newUser.getUsername(), newUser.getEmail());
            return new ApiResponse<User>(400, "Email or Username already exists", newUser);
        }
    }

    @GetMapping("/auth/google/config")
    public ApiResponse<?> getGoogleConfig() {
        boolean effectiveMock = googleMock || isBlank(googleClientId) || isBlank(googleClientSecret);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mock", effectiveMock);
        data.put("clientId", effectiveMock ? "" : googleClientId);

        return new ApiResponse<Map<String, Object>>(200, "Google configuration loaded", data);
    }

    @PostMapping("/auth/google")
    public ApiResponse<?> googleLogin(@RequestBody Map<String, String> request) {
        try {
            String email = trimToNull(request.get("email"));
            String name = trimToNull(request.get("name"));
            String avatarUrl = trimToNull(request.get("picture"));

            if (email == null) {
                String code = trimToNull(request.get("code"));
                String redirectUri = trimToNull(request.get("redirectUri"));

                if (code == null || redirectUri == null) {
                    return new ApiResponse<String>(400, "Missing Google authentication data", "");
                }

                if (isBlank(googleClientId) || isBlank(googleClientSecret)) {
                    return new ApiResponse<String>(400, "Google OAuth is not configured on the server", "");
                }

                GoogleProfile profile = exchangeGoogleCode(code, redirectUri);
                email = profile.email();
                name = profile.name();
                avatarUrl = profile.picture();
            }

            if (isBlank(email)) {
                return new ApiResponse<String>(400, "Google account email is missing", "");
            }

            boolean isNewGoogleRegistration = !rq.googleUserExistsByEmail(email);
            User user = rq.findOrCreateGoogleUser(email, name, avatarUrl);
            if (user.isBanned()) {
                logger.warn("Google login blocked because user {} is banned", email);
                return new ApiResponse<String>(403, "Account has been banned", "");
            }

            String token = sessionManager.createSession(user);
            user.setSessionToken(token);
                        logger.info("Google login successful for {}", email);
            if (isNewGoogleRegistration) {
                try {
                    String timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    String emailBody = EmailTemplateBuilder.buildGoogleRegisterEmail(user.getFullname(), user.getEmail(), timeStr);
                    emailServer.SendEmail(user.getEmail(), "Welcome to BidPop - Account Registration Successful", emailBody);
                } catch (Exception emailError) {
                    logger.error("Failed to send Google registration email notification", emailError);
                }
            }
            return new ApiResponse<User>(200, "Google login successful", user);
        } catch (Exception e) {
            logger.error("Google login failed", e);
            return new ApiResponse<String>(500, "Google login failed: " + e.getMessage(), "");
        }
    }

    private GoogleProfile exchangeGoogleCode(String code, String redirectUri) throws Exception {
        String form = "code=" + encode(code)
                + "&client_id=" + encode(googleClientId)
                + "&client_secret=" + encode(googleClientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
            throw new IllegalStateException("Google token exchange failed");
        }

        JSONObject tokenJson = new JSONObject(tokenResponse.body());
        String accessToken = tokenJson.optString("access_token", "");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("Google access token is missing");
        }

        HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> userInfoResponse = httpClient.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
        if (userInfoResponse.statusCode() < 200 || userInfoResponse.statusCode() >= 300) {
            throw new IllegalStateException("Google user info request failed");
        }

        JSONObject userInfo = new JSONObject(userInfoResponse.body());
        String email = userInfo.optString("email", "");
        String name = userInfo.optString("name", email);
        String picture = userInfo.optString("picture", "");

        if (email.isBlank()) {
            throw new IllegalStateException("Google user info does not contain email");
        }

        return new GoogleProfile(email, name, picture);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleProfile(String email, String name, String picture) {
    }
}
