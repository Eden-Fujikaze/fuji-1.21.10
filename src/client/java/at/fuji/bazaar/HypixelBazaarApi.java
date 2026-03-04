package at.fuji.bazaar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HypixelBazaarApi {
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String ITEM_ENDPOINT = "https://api.hypixel.net/v2/resources/skyblock/items";

    private static final ConcurrentHashMap<String, Double> priceCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> itemNameMap = new ConcurrentHashMap<>();

    /**
     * NPC sell prices loaded from the items endpoint field {@code npc_sell_price}.
     * Keyed by product ID (e.g. "ENCHANTED_SUGAR").
     * 0.0 means the item has no NPC sell value.
     */
    private static final ConcurrentHashMap<String, Double> npcPriceMap = new ConcurrentHashMap<>();

    private static JsonObject cachedJson = null;
    private static JsonObject cachedItemsJson = null;

    private static long lastFetchTime = 0;
    private static long lastItemFetchTime = 0;
    private static final long CACHE_TTL_MS = 30_000;

    // ── Item names + NPC prices ───────────────────────────────────────────────

    /**
     * Loads item display names AND npc_sell_price values from the items endpoint.
     * Call once at startup from ClientModInitializer.
     */
    public static CompletableFuture<Void> loadItemNames() {
        return CompletableFuture.runAsync(() -> {
            try {
                String json = fetchUrlSync(ITEM_ENDPOINT);
                JsonArray items = JsonParser.parseString(json)
                        .getAsJsonObject()
                        .getAsJsonArray("items");

                int npcCount = 0;
                for (JsonElement el : items) {
                    JsonObject item = el.getAsJsonObject();
                    String id = item.get("id").getAsString();
                    String name = item.get("name").getAsString();
                    itemNameMap.put(id, name);

                    // npc_sell_price is present only for NPC-sellable items
                    if (item.has("npc_sell_price")) {
                        double npcPrice = item.get("npc_sell_price").getAsDouble();
                        if (npcPrice > 0) {
                            npcPriceMap.put(id, npcPrice);
                            npcCount++;
                        }
                    }
                }
                System.out.println("[BazaarApi] Loaded " + itemNameMap.size()
                        + " item names, " + npcCount + " NPC sell prices.");
            } catch (Exception e) {
                System.err.println("[BazaarApi] Failed to load item names: " + e.getMessage());
            }
        });
    }

    public static String getItemName(String productId) {
        return itemNameMap.getOrDefault(productId, productId);
    }

    public static String toProductId(String displayName) {
        return displayName.trim().toUpperCase().replace(" ", "_");
    }

    /**
     * Returns the NPC sell price for {@code itemId} loaded from the items endpoint,
     * or {@code 0.0} if the item has no NPC sell value.
     *
     * <p>
     * This is the source of truth for NPC sell prices — the bazaar
     * {@code quick_status} object does NOT contain this field.
     */
    public static double getNpcSellPriceSync(String itemId) {
        return npcPriceMap.getOrDefault(itemId, 0.0);
    }

    /**
     * Async wrapper around {@link #getNpcSellPriceSync} for use in
     * {@code BazaarWorker.fetchNpcPriceAndCheck()}.
     */
    public static CompletableFuture<Double> getNpcSellPrice(String itemId) {
        return CompletableFuture.supplyAsync(() -> {
            double price = getNpcSellPriceSync(itemId);
            System.out.println("[BazaarApi] npcSellPrice(" + itemId + ") = " + price);
            return price;
        });
    }

    // ── Bazaar prices ─────────────────────────────────────────────────────────

    public static JsonObject getAllProductsSync() {
        try {
            refreshBazaarApi();
            if (cachedJson == null)
                return null;
            return cachedJson.getAsJsonObject("products");
        } catch (Exception e) {
            System.err.println("[BazaarApi] getAllProductsSync failed: " + e.getMessage());
            return null;
        }
    }

    public static CompletableFuture<Double> getBuyOrderPrice(String itemId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                refreshBazaarApi();
                if (cachedJson == null)
                    return -1.0;

                JsonObject products = cachedJson.getAsJsonObject("products");
                if (products == null || !products.has(itemId)) {
                    System.err.println("[BazaarApi] Item not found: " + itemId);
                    return -1.0;
                }

                double buyPrice = products
                        .getAsJsonObject(itemId)
                        .getAsJsonObject("quick_status")
                        .get("buyPrice").getAsDouble();

                priceCache.put(itemId, buyPrice);
                return buyPrice;

            } catch (Exception e) {
                System.err.println("[BazaarApi] Failed to fetch price for " + itemId + ": " + e.getMessage());
                return -1.0;
            }
        });
    }

    public static double getBuyOrderPriceSync(String itemId) {
        try {
            refreshBazaarApi();
            if (cachedJson == null)
                return -1.0;

            JsonObject products = cachedJson.getAsJsonObject("products");
            if (products == null || !products.has(itemId))
                return -1.0;

            return products
                    .getAsJsonObject(itemId)
                    .getAsJsonObject("quick_status")
                    .get("buyPrice").getAsDouble();

        } catch (Exception e) {
            System.err.println("[BazaarApi] Sync fetch failed for " + itemId + ": " + e.getMessage());
            return -1.0;
        }
    }

    // ── Internal refresh ──────────────────────────────────────────────────────

    private static synchronized void refreshBazaarApi() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedJson != null && (now - lastFetchTime) < CACHE_TTL_MS)
            return;

        cachedJson = JsonParser.parseString(fetchUrlSync(BAZAAR_URL)).getAsJsonObject();
        lastFetchTime = now;
        System.out.println("[BazaarApi] Bazaar cache refreshed.");
    }

    private static synchronized void refreshItemApi() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedItemsJson != null && (now - lastItemFetchTime) < CACHE_TTL_MS)
            return;

        cachedItemsJson = JsonParser.parseString(fetchUrlSync(ITEM_ENDPOINT)).getAsJsonObject();
        lastItemFetchTime = now;
    }

    private static String fetchUrlSync(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Fuji/1.0");

        if (conn.getResponseCode() != 200)
            throw new RuntimeException("HTTP " + conn.getResponseCode());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
        }
        return sb.toString();
    }
}