package com.auction.server.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailServer {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpClient httpClient = HttpClient.newHttpClient();

    // Auto-detect available processor count for thread pool
    private final int coreCount = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = Executors.newFixedThreadPool(coreCount);

    private static final Logger logger = LoggerFactory.getLogger(EmailServer.class);

    public void SendEmail(String toEmail, String subject, String body) {
        executorService.submit(() -> {
            try {
                Map<String, Object> emailRequest = new HashMap<>();

                Map<String, String> sender = new HashMap<>();
                sender.put("name", senderName);
                sender.put("email", senderEmail);
                emailRequest.put("sender", sender);

                List<Map<String, String>> toList = new ArrayList<>();
                Map<String, String> recipient = new HashMap<>();
                recipient.put("email", toEmail);
                toList.add(recipient);
                emailRequest.put("to", toList);

                emailRequest.put("subject", subject);
                emailRequest.put("htmlContent", body);

                String requestBody = objectMapper.writeValueAsString(emailRequest);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("accept", "application/json")
                        .header("api-key", apiKey)
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("Email sent successfully to: {}", toEmail);
                } else {
                    logger.error("Failed to send email to: {}. Status code: {}, Response: {}", toEmail, response.statusCode(), response.body());
                }

            }
            catch (Exception e) {
                logger.error("Error packaging and sending email: {}", e.getMessage(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}
