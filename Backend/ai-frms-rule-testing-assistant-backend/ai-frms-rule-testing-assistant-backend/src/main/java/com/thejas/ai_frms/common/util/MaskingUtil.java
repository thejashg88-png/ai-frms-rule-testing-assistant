package com.thejas.ai_frms.common.util;

public final class MaskingUtil {

    private MaskingUtil() {
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) {
            return "****";
        }

        String firstSix = cardNumber.substring(0, 6);
        String lastFour = cardNumber.substring(cardNumber.length() - 4);

        return firstSix + "******" + lastFour;
    }

    public static String maskTrack2(String track2) {
        if (track2 == null || track2.isBlank()) {
            return "****";
        }

        int separatorIndex = track2.indexOf('=');

        if (separatorIndex == -1) {
            separatorIndex = track2.indexOf('D');
        }

        if (separatorIndex == -1) {
            return maskCardNumber(track2);
        }

        String pan = track2.substring(0, separatorIndex);
        return maskCardNumber(pan) + track2.substring(separatorIndex, Math.min(track2.length(), separatorIndex + 5)) + "****";
    }

    public static String maskKsn(String ksn) {
        if (ksn == null || ksn.length() < 8) {
            return "****";
        }

        String lastFour = ksn.substring(ksn.length() - 4);
        return "********" + lastFour;
    }

    public static String maskValue(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }

        if (value.length() <= 4) {
            return "****";
        }

        return "****" + value.substring(value.length() - 4);
    }
}