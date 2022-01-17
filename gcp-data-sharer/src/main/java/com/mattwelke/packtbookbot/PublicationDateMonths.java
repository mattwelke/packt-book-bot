package com.mattwelke.packtbookbot;

/**
 * Normalizes publication date month names.
 */
public class PublicationDateMonths {
    /**
     * Given a month's full month name, returns a two-digit string representing the
     * number of the month.
     * 
     * Example: September -> "09"
     * 
     * @param fullMonthName The full month name.
     * @return The two-digit string for the number of the month.
     */
    public static String monthNumber(final String fullMonthName) {
        return switch (fullMonthName.toLowerCase()) {
            case "january" -> "01";
            case "february" -> "02";
            case "march" -> "03";
            case "april" -> "04";
            case "may" -> "05";
            case "june" -> "06";
            case "july" -> "07";
            case "august" -> "08";
            case "september" -> "09";
            case "october" -> "10";
            case "november" -> "11";
            case "december" -> "12";
            default -> throw new IllegalStateException("Invalid full month name: " + fullMonthName);
        };
    }
}
