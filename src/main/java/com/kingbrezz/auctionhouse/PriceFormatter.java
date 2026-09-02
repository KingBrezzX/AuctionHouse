package com.kingbrezz.auctionhouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class PriceFormatter {

    private PriceFormatter() {
    }

    /**
     * Converts a numeric price into a compact format.
     *
     * Examples:
     * 1000        -> 1k
     * 1500        -> 1.5k
     * 1000000     -> 1m
     * 2500000     -> 2.5m
     * 1000000000  -> 1b
     * 1000000000000 -> 1t
     */
    public static String format(double amount) {
        if (!Double.isFinite(amount)) {
            return "0";
        }

        if (amount < 0) {
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

        return plain(amount);
    }

    private static String compact(double value, String suffix) {
        BigDecimal number = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.DOWN)
                .stripTrailingZeros();

        return number.toPlainString() + suffix;
    }

    private static String plain(double value) {
        BigDecimal number = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.DOWN)
                .stripTrailingZeros();

        return number.toPlainString();
    }

    /**
     * Parses:
     *
     * 1000
     * 1k
     * 1.5k
     * 1m
     * 25m
     * 1b
     * 2.5b
     * 1t
     *
     * Commas and spaces are also accepted.
     */
    public static double parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Price cannot be null.");
        }

        String value = input
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(",", "")
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
                // Plain number.
            }
        }

        double number;

        try {
            number = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid price: " + input);
        }

        if (!Double.isFinite(number) || number < 0D) {
            throw new IllegalArgumentException("Invalid price: " + input);
        }

        double result = number * multiplier;

        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Price is too large.");
        }

        return result;
    }

    public static boolean isWithinRange(
            double price,
            double minimum,
            double maximum
    ) {
        return Double.isFinite(price)
                && price >= minimum
                && price <= maximum;
    }
    }
