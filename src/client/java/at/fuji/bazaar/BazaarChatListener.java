package at.fuji.bazaar;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for Hypixel bazaar fill messages and drives the flip loop.
 *
 * Strips §X color codes before matching, then uses contains() so the
 * end of the message ("has been filled!", extra lore, etc.) is irrelevant.
 *
 * Matches:
 * "Your Buy Order for {amount}x {itemName}..."
 * "Your Sell Offer for {amount}x {itemName}..."
 */
public class BazaarChatListener {

    // Strips § followed by any single character (color/format codes)
    private static final Pattern COLOR_CODE = Pattern.compile("§.");

    // [\d,]+ handles both "16x" and "71,000x"
    private static final Pattern BUY_FILLED = Pattern.compile(
            "Your Buy Order for ([\\d,]+)x (.+)");
    private static final Pattern SELL_FILLED = Pattern.compile(
            "Your Sell Offer for ([\\d,]+)x (.+)");

    private static boolean registered = false;

    public static void register() {
        if (registered)
            return;
        registered = true;

        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> handleMessage(message));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay)
                handleMessage(message);
        });
    }

    private static void handleMessage(Text message) {
        String raw = message.getString();
        // Strip §X color/format codes
        String text = COLOR_CODE.matcher(raw).replaceAll("");

        if (!text.contains("Your Buy Order for") && !text.contains("Your Sell Offer for"))
            return;

        Matcher m;

        m = BUY_FILLED.matcher(text);
        if (m.find()) {
            int amount = Integer.parseInt(m.group(1).replace(",", ""));
            // Item name is everything after "{amount}x " up to the next space-separated
            // word boundary
            // We only need the portion before "has" to get the clean item name
            String itemName = m.group(2).replaceAll("\\s+has.*", "").trim();
            System.out.println("[BazaarChat] Buy filled: " + amount + "x " + itemName);
            net.minecraft.client.MinecraftClient.getInstance()
                    .execute(() -> BazaarWorker.onBuyFilled(itemName, amount));
            return;
        }

        m = SELL_FILLED.matcher(text);
        if (m.find()) {
            int amount = Integer.parseInt(m.group(1).replace(",", ""));
            String itemName = m.group(2).replaceAll("\\s+has.*", "").trim();
            System.out.println("[BazaarChat] Sell filled: " + amount + "x " + itemName);
            net.minecraft.client.MinecraftClient.getInstance().execute(() -> BazaarWorker.onSellFilled(itemName));
        }
    }
}