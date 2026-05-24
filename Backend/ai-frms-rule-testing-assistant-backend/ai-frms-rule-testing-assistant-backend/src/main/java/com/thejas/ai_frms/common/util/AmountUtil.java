package com.thejas.ai_frms.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountUtil {

    private AmountUtil() {
    }

    public static BigDecimal toMajorAmount(String minorAmount) {
        if (minorAmount == null || minorAmount.isBlank()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(minorAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static String toMinorAmount(BigDecimal majorAmount) {
        if (majorAmount == null) {
            return "0";
        }

        return majorAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    public static boolean isGreaterThan(BigDecimal amount, BigDecimal threshold) {
        if (amount == null || threshold == null) {
            return false;
        }

        return amount.compareTo(threshold) > 0;
    }

    public static boolean isLessThan(BigDecimal amount, BigDecimal threshold) {
        if (amount == null || threshold == null) {
            return false;
        }

        return amount.compareTo(threshold) < 0;
    }

    public static boolean isGreaterThanOrEqual(BigDecimal amount, BigDecimal threshold) {
        if (amount == null || threshold == null) {
            return false;
        }

        return amount.compareTo(threshold) >= 0;
    }
}