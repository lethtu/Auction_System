package com.auction.server.view;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServer {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;
    public void SendEmail(String toEmail, String subject, String body) {
        try {
            // 2. TẠO THƯ CAO CẤP (MimeMessage) ĐỂ HỖ TRỢ TÊN TIẾNG VIỆT VÀ HTML
            MimeMessage message = mailSender.createMimeMessage();

            // Dùng Helper với encoding UTF-8 để chống lỗi font chữ
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Đặt Tên hiển thị cực đẹp và an toàn: "Hệ thống Đấu giá"
            helper.setFrom(senderEmail, "Hệ thống Đấu giá");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // false = gửi dạng chữ (text) bình thường.
            // Sau này bạn muốn gửi giao diện HTML đẹp mắt thì chỉ cần đổi thành true!
            helper.setText(body, false);

            // Lệnh phát thư đi
            mailSender.send(message);
            System.out.println("Đã gửi email thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("Lỗi khi đóng gói và gửi email: " + e.getMessage());
        }
    }
}
