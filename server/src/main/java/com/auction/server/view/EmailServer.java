package com.auction.server.view;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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
<<<<<<< HEAD
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
=======

    // Cập nhật tự set số luồng
    private final int coreCount = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = Executors.newFixedThreadPool(coreCount);

    private static final Logger logger = LoggerFactory.getLogger(EmailServer.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)

    public void SendEmail(String toEmail, String subject, String body) {
        executorService.submit(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(senderEmail, "Hệ thống Đấu giá");
                helper.setTo(toEmail);
                helper.setSubject(subject);

                // false = gửi dạng chữ (text) bình thường.
                // Sau này muốn gửi giao diện HTML đẹp mắt thì chỉ cần đổi thành true
                helper.setText(body, false);
                mailSender.send(message);
<<<<<<< HEAD
                System.out.println("Đã gửi email thành công tới: " + toEmail);

            } catch (Exception e) {
                System.err.println("Lỗi khi đóng gói và gửi email: " + e.getMessage());
=======
                logger.info("Đã gửi email thành công tới: {}", toEmail);

            }
            catch (Exception e) {
                logger.error("Lỗi khi đóng gói và gửi email: {}", e.getMessage(), e);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}
