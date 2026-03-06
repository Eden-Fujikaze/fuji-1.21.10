package at.rewrite.utils;

public class TextUtils {
    public static String stripColor(String input) {
        return input.replaceAll("§.", "").trim();
    }

    public static boolean compareStrings(String x, String y, boolean exact) {
        if (exact) {
            return x.equals(y);
        } else {
            return x.contains(y);
        }
    }
}
