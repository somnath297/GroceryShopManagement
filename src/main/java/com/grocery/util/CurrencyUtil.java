package com.grocery.util;

import java.text.DecimalFormat;

public class CurrencyUtil {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");
    private static String symbol = "₹";

    public static void setSymbol(String s) { symbol = s; }
    public static String getSymbol() { return symbol; }

    /** Format a value as currency, e.g. ₹1,234.50 */
    public static String format(double value) {
        return symbol + FORMAT.format(value);
    }

    /** Format without the symbol, e.g. 1,234.50 */
    public static String formatPlain(double value) {
        return FORMAT.format(value);
    }
}
