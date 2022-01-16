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
}
