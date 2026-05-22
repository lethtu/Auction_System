package com.auction.server.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailServer {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // Auto-detect available processor count for thread pool
    private final int coreCount = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = Executors.newFixedThreadPool(coreCount);

    private static final Logger logger = LoggerFactory.getLogger(EmailServer.class);

    public void SendEmail(String toEmail, String subject, String body) {
        executorService.submit(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(senderEmail, "Auction System");
                helper.setTo(toEmail);
                helper.setSubject(subject);

                // false = send as plain text.
                // To send with beautiful HTML layout, just change to true
                helper.setText(body, false);
                mailSender.send(message);
                logger.info("Email sent successfully to: {}", toEmail);

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
