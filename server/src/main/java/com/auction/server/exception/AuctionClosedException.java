package com.auction.server.exception;

/**
 * Ngoại lệ được ném khi có hành động đặt giá vào một phiên đấu giá đã kết thúc (ENDED) hoặc đã bị hủy (CANCELED).
 * Kế thừa RuntimeException để không bắt buộc caller phải khai báo throws.
 */
public class AuctionClosedException extends RuntimeException {
    private final Integer sessionId;

    public AuctionClosedException(String message, Integer sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public Integer getSessionId() {
        return sessionId;
    }
}
