package com.auction.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionConcurrencyTest {

    @Test
    @DisplayName("Giả lập 100 luồng đặt giá cùng lúc không phụ thuộc MySQL")
    void testConcurrency() throws InterruptedException {
        int numberOfBidders = 100;

        InMemoryAuctionEngine auctionEngine = new InMemoryAuctionEngine(new BigDecimal("1000.00"));

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfBidders);
        CountDownLatch readyLatch = new CountDownLatch(numberOfBidders);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfBidders);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfBidders; i++) {
            BigDecimal bidAmount = new BigDecimal("1000").add(new BigDecimal(i + 1));

            executorService.execute(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    boolean success = auctionEngine.placeBid(bidAmount);

                    if (success) {
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
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        assertEquals(
                numberOfBidders,
                successCount.get() + failCount.get(),
                "Tổng số luồng xử lý phải đúng bằng 100"
        );

        assertTrue(
                successCount.get() >= 1,
                "Phải có ít nhất 1 lượt đặt giá thành công"
        );

        assertEquals(
                new BigDecimal("1100"),
                auctionEngine.getCurrentPrice(),
                "Giá cuối cùng phải là giá cao nhất"
        );
    }

    private static class InMemoryAuctionEngine {
        private final AtomicReference<BigDecimal> currentPrice;

        InMemoryAuctionEngine(BigDecimal initialPrice) {
            this.currentPrice = new AtomicReference<>(initialPrice);
        }

        synchronized boolean placeBid(BigDecimal bidAmount) {
            if (bidAmount.compareTo(currentPrice.get()) <= 0) {
                return false;
            }

            currentPrice.set(bidAmount);
            return true;
        }

        BigDecimal getCurrentPrice() {
            return currentPrice.get();
        }
    }
}