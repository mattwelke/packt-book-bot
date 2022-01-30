package com.mattwelke.packtbookbot;

/**
 * Normalizes publication date month names.
 */
public class PublicationDateMonths {
    /**
     * Given a month's short month name used on the Packt website, returns the full
     * month name.
     *
     * @param shortMonthName The short month name.
     * @return The full month name for the month.
     */
    public static String monthName(final String shortMonthName) {
        return switch (shortMonthName.toLowerCase()) {
            case "jan" -> "January";
            case "feb" -> "February";
            case "mar" -> "March";
            case "apr" -> "April";
            case "may" -> "May";
            case "jun" -> "June";
            case "jul" -> "July";
            case "aug" -> "August";
            case "sep" -> "September";
            case "oct" -> "October";
            case "nov" -> "November";
            case "dec" -> "December";
            default -> throw new IllegalStateException("Invalid short month name: " + shortMonthName);
        };
    }

    /**
     * Given a month's full month name, returns a two-digit string representing the
     * number of the month.
     * <p>
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
