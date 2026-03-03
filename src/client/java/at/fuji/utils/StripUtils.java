package at.fuji.utils;

public class StripUtils {
    public static String stripColorCodes(String input) {
        return input.replaceAll("§.", "").trim();
    }
}
