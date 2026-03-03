package at.fuji.bazaar;

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
    private static JsonObject cachedItemsJson = null; // Separate cache for items
    private static long lastItemFetchTime = 0;

    private static final ConcurrentHashMap<String, Double> priceCache = new ConcurrentHashMap<>();
    private static long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 30_000; // 30 seconds

    private static JsonObject cachedJson = null;

    /**
     * Returns the full "products" JsonObject from the cached bazaar response.
     * Used by ItemSelector to score all items in one pass without extra HTTP calls.
     */
    public static JsonObject getAllProductsSync() {
        try {
            BaazarApiRefresh();
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
                BaazarApiRefresh();
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
            BaazarApiRefresh();
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

    public static String getItemName(String itemId) {
        try {
            refreshItemApi();
            if (cachedItemsJson == null)
                return "Unknown Item";

            com.google.gson.JsonArray itemsArray = cachedItemsJson.getAsJsonArray("items");

            for (com.google.gson.JsonElement element : itemsArray) {
                JsonObject item = element.getAsJsonObject();
                if (item.get("id").getAsString().equals(itemId)) {
                    return item.get("name").getAsString();
                }
            }
            return itemId; // Fallback to ID if name not found
        } catch (Exception e) {
            System.err.println("[BazaarApi] Failed to get name for " + itemId + ": " + e.getMessage());
            return itemId;
        }
    }

    private static synchronized void refreshItemApi() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedItemsJson != null && (now - lastItemFetchTime) < CACHE_TTL_MS)
            return;

        HttpURLConnection conn = (HttpURLConnection) new URL(ITEM_ENDPOINT).openConnection();
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

        cachedItemsJson = JsonParser.parseString(sb.toString()).getAsJsonObject();
        lastItemFetchTime = now;
    }

    private static synchronized void BaazarApiRefresh() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedJson != null && (now - lastFetchTime) < CACHE_TTL_MS)
            return;

        HttpURLConnection conn = (HttpURLConnection) new URL(BAZAAR_URL).openConnection();
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

        cachedJson = JsonParser.parseString(sb.toString()).getAsJsonObject();
        lastFetchTime = now;
        System.out.println("[BazaarApi] Cache refreshed.");
    }

    public static String toProductId(String displayName) {
        return displayName.trim().toUpperCase().replace(" ", "_");
    }
}