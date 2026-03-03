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
            refreshCacheIfNeeded();
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
                refreshCacheIfNeeded();
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
            refreshCacheIfNeeded();
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

    private static synchronized void refreshCacheIfNeeded() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedJson != null && (now - lastFetchTime) < CACHE_TTL_MS)
            return;

        HttpURLConnection conn = (HttpURLConnection) new URL(BAZAAR_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "FujiBazaarBot/1.0");

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