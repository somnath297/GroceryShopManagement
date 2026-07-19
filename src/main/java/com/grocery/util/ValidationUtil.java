package com.grocery.util;

/**
 * Input validation helpers — all methods return user-friendly error messages or null if valid.
 */
public class ValidationUtil {

    public static String requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return "Please enter " + fieldName + ".";
        }
        return null;
    }

    public static String requirePositiveDouble(String value, String fieldName) {
        if (value == null || value.isBlank()) return "Please enter " + fieldName + ".";
        try {
            double d = Double.parseDouble(value.trim());
            if (d < 0) return fieldName + " cannot be negative.";
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid number.";
        }
        return null;
    }

    public static String requirePositiveInt(String value, String fieldName) {
        if (value == null || value.isBlank()) return "Please enter " + fieldName + ".";
        try {
            int i = Integer.parseInt(value.trim());
            if (i < 0) return fieldName + " cannot be negative.";
        } catch (NumberFormatException e) {
            return fieldName + " must be a whole number.";
        }
        return null;
    }

    public static String requireMobile(String value) {
        if (value == null || value.isBlank()) return null; // mobile is optional
        if (!value.trim().matches("\\d{10}")) {
            return "Mobile number must be 10 digits.";
        }
        return null;
    }

    public static double parseDouble(String value) {
        try { return Double.parseDouble(value.trim()); } catch (Exception e) { return 0; }
    }

    public static int parseInt(String value) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return 0; }
    }
}
