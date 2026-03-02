package at.fuji.utils;

import net.minecraft.network.protocol.game.ClientboundSetScorePacket;

public class ScoreboardUtil {

    private static String location = "N/A";
    private static long purse = 0;

    public static void handlePacket(ClientboundSetScorePacket packet) {
        String line = packet.owner();
        if (line == null || line.isEmpty())
            return;

        String clean = line.replaceAll("§.", "").trim();

        if (clean.toLowerCase().startsWith("location:")) {
            location = clean.substring(clean.indexOf(":") + 1).trim();
            System.out.println("[Hypixel] Location: " + location);
        } else if (clean.toLowerCase().startsWith("purse:")) {
            String coinsText = clean.substring(clean.indexOf(":") + 1).trim();
            coinsText = coinsText.replaceAll("[^\\d,]", ""); // strip symbols
            coinsText = coinsText.replace(",", "");
            try {
                purse = Long.parseLong(coinsText);
                System.out.println("[Hypixel] Purse: " + purse);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static String getLocation() {
        return location;
    }

    public static long getPurse() {
        return purse;
    }
}