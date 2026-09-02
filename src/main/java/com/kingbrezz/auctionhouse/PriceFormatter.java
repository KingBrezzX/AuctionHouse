package com.kingbrezz.auctionhouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class PriceFormatter {

    private PriceFormatter() {
    }

    public static double parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Price cannot be null.");
        }

        String value = input
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(",", "")
                .replace("_", "")
                .replace(" ", "");

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Price cannot be empty.");
        }

        double multiplier = 1D;

        char last = value.charAt(value.length() - 1);

        switch (last) {
            case 'k' -> {
                multiplier = 1_000D;
                value = value.substring(0, value.length() - 1);
            }
            case 'm' -> {
                multiplier = 1_000_000D;
                value = value.substring(0, value.length() - 1);
            }
            case 'b' -> {
                multiplier = 1_000_000_000D;
                value = value.substring(0, value.length() - 1);
            }
            case 't' -> {
                multiplier = 1_000_000_000_000D;
                value = value.substring(0, value.length() - 1);
            }
            default -> {
            }
        }

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Invalid price.");
        }

        double number;

        try {
            number = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid price.");
        }

        if (!Double.isFinite(number) || number < 0D) {
            throw new IllegalArgumentException("Invalid price.");
        }

        double result = number * multiplier;

        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Price is too large.");
        }

        return result;
    }

    public static String format(double amount) {
        if (!Double.isFinite(amount)) {
            return "0";
        }

        if (amount < 0D) {
            return "-" + format(-amount);
        }

        if (amount >= 1_000_000_000_000D) {
            return compact(amount / 1_000_000_000_000D, "t");
        }

        if (amount >= 1_000_000_000D) {
            return compact(amount / 1_000_000_000D, "b");
        }

        if (amount >= 1_000_000D) {
            return compact(amount / 1_000_000D, "m");
        }

        if (amount >= 1_000D) {
            return compact(amount / 1_000D, "k");
        }

        BigDecimal decimal = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.DOWN)
                .stripTrailingZeros();

        return decimal.toPlainString();
    }

    private static String compact(
            double value,
            String suffix
    ) {
        BigDecimal decimal = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.DOWN)
                .stripTrailingZeros();

        return decimal.toPlainString() + suffix;
    }

    public static boolean isValidRange(
            double price,
            double minimum,
            double maximum
    ) {
        return Double.isFinite(price)
                && price >= minimum
                && price <= maximum;
    }
}
