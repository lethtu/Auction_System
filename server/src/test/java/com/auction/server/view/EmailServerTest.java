package com.auction.server.view;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServerTest {

    @Mock
    private JavaMailSender mailSender; // Đóng giả server mail

    @InjectMocks
    private EmailServer emailServer; // Bơm mailSender giả vào đây

    @BeforeEach
    void setUp() {
        // Vì class dùng @Value cho senderEmail, ta dùng Reflection để set giá trị giả
        ReflectionTestUtils.setField(emailServer, "senderEmail", "noreply@auction.com");

        // Không mock MimeMessage trực tiếp vì Mockito/Byte Buddy có thể lỗi trên JDK mới.
        // Dùng MimeMessage thật, còn JavaMailSender vẫn là mock.
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    @DisplayName("Test: Gửi email thành công (Async)")
    public void testSendEmail_Success() {
        String to = "customer@gmail.com";
        String subject = "Chào mừng";
        String body = "Nội dung đấu giá";

        // Hành động
        emailServer.SendEmail(to, subject, body);

        // Vì dùng executorService.submit (đa luồng) nên cần Awaitility để đợi
        // Nó sẽ kiểm tra mỗi 100ms, tối đa 2 giây cho đến khi điều kiện verify đúng
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        });
    }

    @Test
    @DisplayName("Test: Gửi email thất bại (Lỗi Server mail)")
    public void testSendEmail_Failure() {
        // Giả lập kịch bản: Khi gọi hàm send() thì văng Exception
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(MimeMessage.class));

        // Hành động
        emailServer.SendEmail("fail@test.com", "Test", "Body");

        // Đợi xem code có nhảy vào khối catch và không bị sập app không
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            // Xác nhận là hàm send đã được gọi nhưng gặp lỗi
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        });

        // Bài test này pass nếu không có exception nào văng ra làm chết luồng chính
    }
}