package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts a rupee amount to Indian-style words.
 * e.g. 10590.00 → "Rupees Ten Thousand Five Hundred and Ninety Only"
 */
public class AmountInWords {

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(BigDecimal amount) {
        if (amount == null) return "Rupees Zero Only";
        long rupees = amount.setScale(0, RoundingMode.FLOOR).longValue();
        int paise = amount.subtract(BigDecimal.valueOf(rupees))
                          .multiply(BigDecimal.valueOf(100))
                          .setScale(0, RoundingMode.HALF_UP).intValue();

        StringBuilder sb = new StringBuilder("Rupees ");
        sb.append(inWords(rupees));
        if (paise > 0) {
            sb.append(" and ").append(inWords(paise)).append(" Paise");
        }
        sb.append(" Only");
        return sb.toString();
    }

    private static String inWords(long n) {
        if (n == 0) return "Zero";
        if (n < 0)  return "Minus " + inWords(-n);

        StringBuilder s = new StringBuilder();
        if (n >= 10_00_00_000L) { s.append(inWords(n / 10_00_00_000L)).append(" Arab "); n %= 10_00_00_000L; }
        if (n >= 1_00_00_000L)  { s.append(inWords(n / 1_00_00_000L)).append(" Crore "); n %= 1_00_00_000L; }
        if (n >= 1_00_000L)     { s.append(inWords(n / 1_00_000L)).append(" Lakh "); n %= 1_00_000L; }
        if (n >= 1_000L)        { s.append(inWords(n / 1_000L)).append(" Thousand "); n %= 1_000L; }
        if (n >= 100L)          { s.append(ONES[(int)(n / 100)]).append(" Hundred "); n %= 100; }
        if (n > 0) {
            if (s.length() > 0) s.append("and ");
            if (n < 20) { s.append(ONES[(int) n]); }
            else { s.append(TENS[(int)(n / 10)]); if (n % 10 > 0) s.append(" ").append(ONES[(int)(n % 10)]); }
        }
        return s.toString().trim();
    }
}
