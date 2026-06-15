package com.spendvision.app.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyUtils {

    private CurrencyUtils() {
    }

    public static String findMoneyInLine(String line) {
        Pattern pattern = Pattern.compile("£?\\s?\\d{1,4}[.,]\\d{2}");
        Matcher matcher = pattern.matcher(line);

        String lastAmount = "";

        while (matcher.find()) {
            lastAmount = formatMoneyText(matcher.group());
        }

        return lastAmount;
    }

    public static String formatMoneyText(String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) {
            return "";
        }

        String cleaned = amountText
                .replace("£", "")
                .replace(" ", "")
                .replace(",", ".")
                .trim();

        return "£" + cleaned;
    }

    public static String formatCurrency(double amount) {
        return "£" + String.format(Locale.UK, "%.2f", amount);
    }

    public static double cleanAmount(String amountText) {
        try {
            if (amountText == null) {
                return 0;
            }

            String cleaned = amountText
                    .replace("£", "")
                    .replace(",", "")
                    .replace(" ", "")
                    .trim();

            return Double.parseDouble(cleaned);

        } catch (Exception e) {
            return 0;
        }
    }
}