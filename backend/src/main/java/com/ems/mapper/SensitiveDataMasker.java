package com.ems.mapper;

/** Formatting / masking for Aadhaar (stored as 12 digits) and PAN (stored as 10 upper-case chars). */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    /** 123456789012 -> 1234-5678-9012 */
    public static String formatAadhaar(String digits) {
        return digits.substring(0, 4) + "-" + digits.substring(4, 8) + "-" + digits.substring(8);
    }

    /** 123456789012 -> XXXX-XXXX-9012 */
    public static String maskAadhaar(String digits) {
        return "XXXX-XXXX-" + digits.substring(8);
    }

    /** ABCDE1234F -> XXXXXX234F */
    public static String maskPan(String pan) {
        return "XXXXXX" + pan.substring(6);
    }
}
