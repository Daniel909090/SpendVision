package com.spendvision.app.services;

import com.spendvision.app.utils.CurrencyUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParserService {

    public ParsedReceipt parseReceipt(String text) {
        String total = findTotal(text);
        String category = detectCategory(text);
        String store = detectStore(text);
        String date = detectDate(text);
        String items = buildItemsList(text);

        return new ParsedReceipt(store, date, total, category, items);
    }

    private String findTotal(String text) {
        String[] lines = text.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            String upperLine = lines[i].trim().toUpperCase();

            boolean isTotalLine =
                    upperLine.equals("TOTAL")
                            || upperLine.equals("TOTAL:")
                            || upperLine.contains("TOTAL TO PAY")
                            || upperLine.contains("AMOUNT DUE")
                            || upperLine.contains("BALANCE DUE")
                            || upperLine.contains("GRAND TOTAL");

            boolean ignoreLine =
                    upperLine.contains("SAVING")
                            || upperLine.contains("DISCOUNT")
                            || upperLine.contains("VAT")
                            || upperLine.contains("SUBTOTAL")
                            || upperLine.contains("ITEM")
                            || upperLine.contains("QTY")
                            || upperLine.contains("CHANGE");

            if (isTotalLine && !ignoreLine) {
                for (int j = i; j <= i + 5 && j < lines.length; j++) {
                    String amount = CurrencyUtils.findMoneyInLine(lines[j]);

                    if (!amount.equals("")) {
                        return amount;
                    }
                }
            }
        }

        for (int i = 0; i < lines.length; i++) {
            String upperLine = lines[i].trim().toUpperCase();

            if (upperLine.contains("VISA")
                    || upperLine.contains("MASTERCARD")
                    || upperLine.contains("CARD")
                    || upperLine.contains("CONTACTLESS")) {

                for (int j = i; j <= i + 4 && j < lines.length; j++) {
                    String amount = CurrencyUtils.findMoneyInLine(lines[j]);

                    if (!amount.equals("")) {
                        return amount;
                    }
                }
            }
        }

        return findLargestAmount(text);
    }

    private String findLargestAmount(String text) {
        String[] lines = text.split("\\n");

        double largest = 0;
        String largestText = "";

        for (String line : lines) {
            String upperLine = line.toUpperCase();

            if (upperLine.contains("SAVING")
                    || upperLine.contains("DISCOUNT")
                    || upperLine.contains("CHANGE")
                    || upperLine.contains("VAT")
                    || upperLine.contains("SUBTOTAL")) {
                continue;
            }

            Pattern pattern = Pattern.compile("£?\\s?\\d+\\.\\d{2}");
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                double amount = CurrencyUtils.cleanAmount(matcher.group());

                if (amount > largest) {
                    largest = amount;
                    largestText = CurrencyUtils.formatCurrency(amount);
                }
            }
        }

        if (!largestText.equals("")) {
            return largestText;
        }

        return "Total not found";
    }

    private String buildItemsList(String text) {
        StringBuilder items = new StringBuilder();
        String[] lines = text.split("\\n");

        Pattern pricePattern = Pattern.compile("£?\\s?\\d+\\.\\d{2}");

        for (String line : lines) {
            String cleanLine = line.trim();
            Matcher matcher = pricePattern.matcher(cleanLine);

            if (matcher.find()) {
                String rawPrice = matcher.group();
                String price = CurrencyUtils.formatMoneyText(rawPrice);

                String product =
                        cleanLine.replace(rawPrice, "")
                                .replace("£", "")
                                .replace("GBP", "")
                                .replace("gbp", "")
                                .trim();

                String upperProduct = product.toUpperCase();

                if (!product.isEmpty()
                        && product.length() > 2
                        && !upperProduct.contains("TOTAL")
                        && !upperProduct.contains("BALANCE")
                        && !upperProduct.contains("AMOUNT")
                        && !upperProduct.contains("CARD")
                        && !upperProduct.contains("CHANGE")
                        && !upperProduct.contains("VAT")
                        && !upperProduct.contains("SAVING")
                        && !upperProduct.contains("DISCOUNT")
                        && !upperProduct.contains("VISA")
                        && !upperProduct.contains("MASTERCARD")
                        && !upperProduct.contains("CONTACTLESS")) {

                    items.append("• ")
                            .append(product)
                            .append("    ")
                            .append(price)
                            .append("\n");
                }
            }
        }

        if (items.length() == 0) {
            return "No item-price pairs found. Try a clearer, straighter photo.";
        }

        return items.toString();
    }

    private String detectCategory(String text) {
        String upperText = text.toUpperCase();

        if (upperText.contains("TESCO")
                || upperText.contains("ASDA")
                || upperText.contains("LIDL")
                || upperText.contains("ALDI")
                || upperText.contains("MORRISONS")
                || upperText.contains("SAINSBURY")
                || upperText.contains("ICELAND")
                || upperText.contains("CO-OP")
                || upperText.contains("COOP")
                || upperText.contains("WAITROSE")
                || upperText.contains("MARKS AND SPENCER")
                || upperText.contains("M&S")
                || upperText.contains("FOOD WAREHOUSE")) {
            return "Groceries";
        }

        if (upperText.contains("SHELL")
                || upperText.contains("BP")
                || upperText.contains("ESSO")
                || upperText.contains("TEXACO")
                || upperText.contains("JET")
                || upperText.contains("GULF")
                || upperText.contains("FUEL")
                || upperText.contains("PETROL")
                || upperText.contains("DIESEL")) {
            return "Fuel";
        }

        if (upperText.contains("MCDONALD")
                || upperText.contains("KFC")
                || upperText.contains("BURGER KING")
                || upperText.contains("SUBWAY")
                || upperText.contains("NANDOS")
                || upperText.contains("DOMINO")
                || upperText.contains("PIZZA HUT")
                || upperText.contains("GREGGS")
                || upperText.contains("COSTA")
                || upperText.contains("STARBUCKS")
                || upperText.contains("RESTAURANT")
                || upperText.contains("PIZZA")) {
            return "Restaurant";
        }

        if (upperText.contains("BOOTS")
                || upperText.contains("PHARMACY")
                || upperText.contains("SUPERDRUG")
                || upperText.contains("HOLLAND & BARRETT")
                || upperText.contains("HOLLAND AND BARRETT")) {
            return "Health";
        }

        if (upperText.contains("TK MAXX")
                || upperText.contains("TKMAXX")
                || upperText.contains("PRIMARK")
                || upperText.contains("NEXT")
                || upperText.contains("H&M")
                || upperText.contains("ZARA")
                || upperText.contains("NEW LOOK")
                || upperText.contains("RIVER ISLAND")
                || upperText.contains("SPORTS DIRECT")
                || upperText.contains("JD SPORTS")
                || upperText.contains("SCHUH")
                || upperText.contains("CLARKS")) {
            return "Clothing";
        }

        if (upperText.contains("SMYTHS")
                || upperText.contains("THE ENTERTAINER")
                || upperText.contains("TOY")
                || upperText.contains("GAME")) {
            return "Toys";
        }

        if (upperText.contains("B&Q")
                || upperText.contains("B AND Q")
                || upperText.contains("WICKES")
                || upperText.contains("SCREWFIX")
                || upperText.contains("TOOLSTATION")
                || upperText.contains("HOMEBASE")
                || upperText.contains("GARDEN CENTRE")
                || upperText.contains("DOBBIES")) {
            return "Home & DIY";
        }

        if (upperText.contains("ARGOS")
                || upperText.contains("CURRYS")
                || upperText.contains("CURRY'S")
                || upperText.contains("AMAZON")
                || upperText.contains("APPLE")
                || upperText.contains("SAMSUNG")
                || upperText.contains("CE X")
                || upperText.contains("CEX")) {
            return "Electronics";
        }

        if (upperText.contains("POUNDLAND")
                || upperText.contains("B&M")
                || upperText.contains("B AND M")
                || upperText.contains("HOME BARGAINS")
                || upperText.contains("THE RANGE")
                || upperText.contains("DUNELM")
                || upperText.contains("WILKO")) {
            return "Household";
        }

        return "Other";
    }

    private String detectStore(String text) {
        String upperText = text.toUpperCase();

        if (upperText.contains("TESCO")) return "Tesco";
        if (upperText.contains("LIDL")) return "Lidl";
        if (upperText.contains("ALDI")) return "Aldi";
        if (upperText.contains("ASDA")) return "Asda";
        if (upperText.contains("MORRISONS")) return "Morrisons";
        if (upperText.contains("SAINSBURY")) return "Sainsbury's";
        if (upperText.contains("ICELAND")) return "Iceland";
        if (upperText.contains("WAITROSE")) return "Waitrose";
        if (upperText.contains("MARKS AND SPENCER") || upperText.contains("M&S")) return "M&S";
        if (upperText.contains("CO-OP") || upperText.contains("COOP")) return "Co-op";
        if (upperText.contains("FOOD WAREHOUSE")) return "Food Warehouse";

        if (upperText.contains("SHELL")) return "Shell";
        if (upperText.contains("BP")) return "BP";
        if (upperText.contains("ESSO")) return "Esso";
        if (upperText.contains("TEXACO")) return "Texaco";
        if (upperText.contains("JET")) return "Jet";
        if (upperText.contains("GULF")) return "Gulf";

        if (upperText.contains("MCDONALD")) return "McDonald's";
        if (upperText.contains("KFC")) return "KFC";
        if (upperText.contains("BURGER KING")) return "Burger King";
        if (upperText.contains("SUBWAY")) return "Subway";
        if (upperText.contains("NANDOS")) return "Nando's";
        if (upperText.contains("DOMINO")) return "Domino's";
        if (upperText.contains("PIZZA HUT")) return "Pizza Hut";
        if (upperText.contains("GREGGS")) return "Greggs";
        if (upperText.contains("COSTA")) return "Costa";
        if (upperText.contains("STARBUCKS")) return "Starbucks";

        if (upperText.contains("BOOTS")) return "Boots";
        if (upperText.contains("SUPERDRUG")) return "Superdrug";
        if (upperText.contains("HOLLAND & BARRETT") || upperText.contains("HOLLAND AND BARRETT")) return "Holland & Barrett";

        if (upperText.contains("TK MAXX") || upperText.contains("TKMAXX")) return "TK Maxx";
        if (upperText.contains("PRIMARK")) return "Primark";
        if (upperText.contains("NEXT")) return "Next";
        if (upperText.contains("H&M")) return "H&M";
        if (upperText.contains("ZARA")) return "Zara";
        if (upperText.contains("NEW LOOK")) return "New Look";
        if (upperText.contains("RIVER ISLAND")) return "River Island";
        if (upperText.contains("SPORTS DIRECT")) return "Sports Direct";
        if (upperText.contains("JD SPORTS")) return "JD Sports";
        if (upperText.contains("SCHUH")) return "Schuh";
        if (upperText.contains("CLARKS")) return "Clarks";

        if (upperText.contains("SMYTHS")) return "Smyths";
        if (upperText.contains("THE ENTERTAINER")) return "The Entertainer";
        if (upperText.contains("GAME")) return "GAME";

        if (upperText.contains("B&Q") || upperText.contains("B AND Q")) return "B&Q";
        if (upperText.contains("WICKES")) return "Wickes";
        if (upperText.contains("SCREWFIX")) return "Screwfix";
        if (upperText.contains("TOOLSTATION")) return "Toolstation";
        if (upperText.contains("HOMEBASE")) return "Homebase";
        if (upperText.contains("DOBBIES")) return "Dobbies";

        if (upperText.contains("ARGOS")) return "Argos";
        if (upperText.contains("CURRYS") || upperText.contains("CURRY'S")) return "Currys";
        if (upperText.contains("AMAZON")) return "Amazon";
        if (upperText.contains("APPLE")) return "Apple";
        if (upperText.contains("SAMSUNG")) return "Samsung";
        if (upperText.contains("CE X") || upperText.contains("CEX")) return "CeX";

        if (upperText.contains("POUNDLAND")) return "Poundland";
        if (upperText.contains("B&M") || upperText.contains("B AND M")) return "B&M";
        if (upperText.contains("HOME BARGAINS")) return "Home Bargains";
        if (upperText.contains("THE RANGE")) return "The Range";
        if (upperText.contains("DUNELM")) return "Dunelm";
        if (upperText.contains("WILKO")) return "Wilko";

        return "Unknown Store";
    }

    private String detectDate(String text) {
        Pattern pattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group();
        }

        return "Unknown Date";
    }

    public static class ParsedReceipt {

        private final String store;
        private final String date;
        private final String total;
        private final String category;
        private final String items;

        public ParsedReceipt(String store, String date, String total, String category, String items) {
            this.store = store;
            this.date = date;
            this.total = total;
            this.category = category;
            this.items = items;
        }

        public String getStore() {
            return store;
        }

        public String getDate() {
            return date;
        }

        public String getTotal() {
            return total;
        }

        public String getCategory() {
            return category;
        }

        public String getItems() {
            return items;
        }
    }
}