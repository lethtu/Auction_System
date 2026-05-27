package com.auction.client.util;

import java.math.BigDecimal;

public final class BidStepPolicy {
    private static final BigDecimal THRESHOLD_100K = new BigDecimal("100000");
    private static final BigDecimal THRESHOLD_500K = new BigDecimal("500000");
    private static final BigDecimal THRESHOLD_1M = new BigDecimal("1000000");
    private static final BigDecimal THRESHOLD_5M = new BigDecimal("5000000");
    private static final BigDecimal THRESHOLD_10M = new BigDecimal("10000000");
    private static final BigDecimal THRESHOLD_50M = new BigDecimal("50000000");

    private static final BigDecimal INCREMENT_10K = new BigDecimal("10000");
    private static final BigDecimal INCREMENT_20K = new BigDecimal("20000");
    private static final BigDecimal INCREMENT_50K = new BigDecimal("50000");
    private static final BigDecimal INCREMENT_100K = new BigDecimal("100000");
    private static final BigDecimal INCREMENT_200K = new BigDecimal("200000");
    private static final BigDecimal INCREMENT_500K = new BigDecimal("500000");
    private static final BigDecimal INCREMENT_1M = new BigDecimal("1000000");

    private BidStepPolicy() {
    }

    public static BigDecimal getDynamicStepPrice(BigDecimal price) {
        if (price == null || price.compareTo(THRESHOLD_100K) < 0) {
            return INCREMENT_10K;
        } else if (price.compareTo(THRESHOLD_500K) < 0) {
            return INCREMENT_20K;
        } else if (price.compareTo(THRESHOLD_1M) < 0) {
            return INCREMENT_50K;
        } else if (price.compareTo(THRESHOLD_5M) < 0) {
            return INCREMENT_100K;
        } else if (price.compareTo(THRESHOLD_10M) < 0) {
            return INCREMENT_200K;
        } else if (price.compareTo(THRESHOLD_50M) < 0) {
            return INCREMENT_500K;
        }
        return INCREMENT_1M;
    }
}
