package at.fuji.bazaar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scores every item in the Hypixel Bazaar against the player's purse and
 * returns the best item to buy-order based on:
 * - profit per item (sellPrice - buyPrice)
 * - profit margin (profit / buyPrice)
 * - total profit (profit * purchasable amount)
 * - purchasable amount (purse / buyPrice, capped by sell supply)
 * - fill speed (sell volume / competing buy orders)
 *
 * Reuses HypixelBazaarApi's cache — no extra HTTP calls if the cache is fresh.
 */
public class ItemSelector {

    // Items to skip — either too illiquid, bugged, or not meaningfully tradeable
    private static final java.util.Set<String> BLACKLIST = java.util.Set.of(
            "BAZAAR_COOKIE", "BOOSTER_COOKIE");

    // Minimum thresholds — filters out noise before scoring
    private static final double MIN_PROFIT_PER_ITEM = 1.0; // at least 1 coin profit
    private static final double MIN_PROFIT_MARGIN = 0.001; // at least 0.1% margin
    private static final double MIN_SELL_MOVING_WEEK = 1_000; // minimal volume requirement
    private static final int MIN_PURCHASABLE = 1;

    /**
     * Asynchronously fetches the full bazaar, scores every item against the
     * given purse value, and returns the best ItemScore.
     * Returns null if scoring fails or no viable item is found.
     */
    public static CompletableFuture<ItemScore> selectBestItem(double purse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject allProducts = HypixelBazaarApi.getAllProductsSync();
                if (allProducts == null) {
                    System.err.println("[ItemSelector] Could not fetch bazaar data.");
                    return null;
                }

                List<ItemScore> candidates = new ArrayList<>();

                int total = 0, badPrice = 0, lowProfit = 0, lowMargin = 0, lowVolume = 0, cantAfford = 0,
                        zeroPurchasable = 0;

                for (String productId : allProducts.keySet()) {
                    if (BLACKLIST.contains(productId))
                        continue;
                    total++;

                    try {
                        JsonObject qs = allProducts
                                .getAsJsonObject(productId)
                                .getAsJsonObject("quick_status");

                        double buyPrice = qs.get("buyPrice").getAsDouble();
                        double sellPrice = qs.get("sellPrice").getAsDouble();
                        double buyMovingWeek = qs.get("buyMovingWeek").getAsDouble();
                        double sellMovingWeek = qs.get("sellMovingWeek").getAsDouble();
                        int buyOrders = qs.get("buyOrders").getAsInt();

                        if (buyPrice <= 0 || sellPrice <= 0) {
                            badPrice++;
                            continue;
                        }

                        double profitPerItem = buyPrice - sellPrice; // spread: ask - bid
                        double profitMargin = profitPerItem / buyPrice;

                        if (profitPerItem < MIN_PROFIT_PER_ITEM) {
                            lowProfit++;
                            continue;
                        }
                        if (profitMargin < MIN_PROFIT_MARGIN) {
                            lowMargin++;
                            continue;
                        }
                        if (sellMovingWeek < MIN_SELL_MOVING_WEEK) {
                            lowVolume++;
                            continue;
                        }
                        if (purse < buyPrice) {
                            cantAfford++;
                            continue;
                        }

                        String displayName = toDisplayName(productId);

                        ItemScore score = new ItemScore(
                                productId, displayName,
                                buyPrice, sellPrice,
                                buyMovingWeek, sellMovingWeek,
                                buyOrders, purse);

                        if (score.purchasableAmount < MIN_PURCHASABLE || score.totalProfit <= 0) {
                            zeroPurchasable++;
                            continue;
                        }

                        candidates.add(score);

                    } catch (Exception e) {
                        // Malformed entry — skip silently
                    }
                }

                System.out.println(String.format(
                        "[ItemSelector] %d total | badPrice=%d lowProfit=%d lowMargin=%d lowVolume=%d cantAfford=%d zeroPurchasable=%d candidates=%d",
                        total, badPrice, lowProfit, lowMargin, lowVolume, cantAfford, zeroPurchasable,
                        candidates.size()));

                if (candidates.isEmpty()) {
                    System.err.println("[ItemSelector] No viable items found.");
                    return null;
                }

                // Sort descending by composite score
                candidates.sort(Comparator.comparingDouble(s -> -s.score));

                // Log top 10
                System.out.println("[ItemSelector] Top candidates for purse " + (long) purse + " (" + candidates.size()
                        + " total):");
                candidates.stream().limit(10).forEach(s -> System.out.println(String.format(
                        "  #%d %s | profit/item=%.1f (%.1f%%) | total=%.0f | amount=%d | fillSpeed=%.0f",
                        candidates.indexOf(s) + 1, s.displayName,
                        s.profitPerItem, s.profitMargin * 100,
                        s.totalProfit, s.purchasableAmount, s.fillSpeed)));

                ItemScore best = candidates.get(0);
                System.out.println("[ItemSelector] Selected: " + best.displayName);
                return best;

            } catch (Exception e) {
                System.err.println("[ItemSelector] Scoring failed: " + e.getMessage());
                return null;
            }
        });
    }

    /** Converts "ENCHANTED_DIAMOND" → "Enchanted Diamond" */
    private static String toDisplayName(String productId) {
        String[] parts = productId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}