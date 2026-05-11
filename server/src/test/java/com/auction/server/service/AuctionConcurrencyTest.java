package com.auction.server.service;

import com.auction.server.dto.BidResponse;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class AuctionConcurrencyTest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Test
    @DisplayName("Giả lập 100 luồng đặt giá cùng lúc dùng chung 5 User có sẵn")
    public void testConcurrency() throws InterruptedException {
        int numberOfBidders = 100;

        int sessionId = 1;

        int[] validUserIds = {10, 12, 14, 15, 16};

        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Session ID: " + sessionId));

        session.setCurrentPrice(new BigDecimal("1000.00"));

        session.setStatus(AuctionStatus.ACTIVE);
        auctionSessionRepository.save(session);

        // 1. Chuẩn bị 100 luồng
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfBidders);
        CountDownLatch readyLatch = new CountDownLatch(numberOfBidders);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfBidders);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfBidders; i++) {
            final int userId = validUserIds[i % validUserIds.length];

            // Mỗi luồng sẽ mang một số tiền khác nhau (từ 1001 đến 1100)
            final BigDecimal bidAmount = new BigDecimal("1000").add(new BigDecimal(i + 1));

            executorService.execute(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // Chờ hiệu lệnh xuất phát

                    // Bóp cò: Lao vào gọi hàm updateBid cùng 1 lúc
                    BidResponse response = auctionService.updateBid(sessionId, userId, bidAmount);

                    if (response.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        System.out.println("Đã nạp 100 luồng. BẮT ĐẦU CHẠY ĐUA...");

        long startTime = System.currentTimeMillis();
        startLatch.countDown(); // MỞ CỬA CHO 100 LUỒNG CHẠY!

        doneLatch.await(); // Chờ tất cả chạy xong
        long endTime = System.currentTimeMillis();

        // 3. In kết quả và Kiểm chứng
        AuctionSession finalSession = auctionSessionRepository.findById(sessionId).orElseThrow();

        System.out.println("====== KẾT QUẢ TEST ĐA LUỒNG ======");
        System.out.println("Thời gian xử lý 100 request: " + (endTime - startTime) + " ms");
        System.out.println("Số lệnh thành công (Cập nhật giá): " + successCount.get());
        System.out.println("Số lệnh bị từ chối (Do giá đến sau bị thấp hơn): " + failCount.get());
        System.out.println("Giá cuối cùng chốt được trong DB: " + finalSession.getCurrentPrice());

        // Kiểm chứng: Tổng số thành công + thất bại phải đúng bằng 100
        assertTrue((successCount.get() + failCount.get()) == 100, "Tổng số luồng xử lý phải là 100");

        // Kiểm chứng: Giá cuối cùng phải >= 1001 (Vì chắc chắn phải có ít nhất 1 lệnh thành công)
        assertTrue(finalSession.getCurrentPrice().compareTo(new BigDecimal("1001")) >= 0, "Giá cuối cùng phải lớn hơn giá khởi điểm");
    }
}