package at.fuji.bazaar;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scores every Hypixel Bazaar item using:
 * weeklyProfit = (buyPrice - sellPrice) * salesPerWeek / 168
 *
 * API field semantics (confusingly named by Hypixel):
 * quick_status.buyPrice = lowest ask (what you pay to instant-buy)
 * quick_status.sellPrice = highest bid (what you get to instant-sell)
 *
 * Spread = buyPrice - sellPrice (what you pocket per flip)
 * salesPerWeek = min(buyMovingWeek, sellMovingWeek) / buyPrice
 */
public class ItemSelector {

    private static final java.util.Set<String> BLACKLIST = java.util.Set.of(
            "BAZAAR_COOKIE", "BOOSTER_COOKIE", "ESSENCE_DIAMOND");

    public static CompletableFuture<ItemScore> selectBestItem(double purse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject allProducts = HypixelBazaarApi.getAllProductsSync();
                if (allProducts == null) {
                    System.err.println("[ItemSelector] Could not fetch bazaar data.");
                    return null;
                }

                List<ItemScore> candidates = new ArrayList<>();
                int total = 0, badPrice = 0, noSpread = 0, cantAfford = 0, manipulated = 0;

                for (String productId : allProducts.keySet()) {
                    if (BLACKLIST.contains(productId))
                        continue;
                    total++;

                    try {
                        JsonObject qs = allProducts
                                .getAsJsonObject(productId)
                                .getAsJsonObject("quick_status");

                        // Hypixel naming: buyPrice = ask, sellPrice = bid
                        double askPrice = qs.get("buyPrice").getAsDouble(); // what you pay
                        double bidPrice = qs.get("sellPrice").getAsDouble(); // what you get
                        double buyMovingWeek = qs.get("buyMovingWeek").getAsDouble();
                        double sellMovingWeek = qs.get("sellMovingWeek").getAsDouble();
                        int buyOrders = qs.get("buyOrders").getAsInt();
                        int sellOrders = qs.get("sellOrders").getAsInt();

                        if (askPrice <= 0 || bidPrice <= 0) {
                            badPrice++;
                            continue;
                        }
                        if (askPrice <= bidPrice) {
                            noSpread++;
                            continue;
                        }
                        if (askPrice - bidPrice < 500) {
                            noSpread++;
                            continue;
                        } // less than 100 coins profit per item
                        if (purse < askPrice) {
                            cantAfford++;
                            continue;
                        }

                        // Core formula
                        double spread = askPrice - bidPrice;
                        double salesPerWeek = Math.min(buyMovingWeek, sellMovingWeek) / askPrice;
                        double weeklyProfit = spread * salesPerWeek / ItemScore.HOURS_PER_WEEK;

                        if (weeklyProfit <= 0)
                            continue;

                        String displayName = toDisplayName(productId);
                        ItemScore score = new ItemScore(
                                productId, displayName,
                                askPrice, bidPrice,
                                buyMovingWeek, sellMovingWeek,
                                buyOrders, sellOrders, purse);

                        if (score.purchasableAmount < 1) {
                            cantAfford++;
                            continue;
                        }
                        if (score.manipulated) {
                            manipulated++;
                            continue;
                        }

                        candidates.add(score);

                    } catch (Exception e) {
                        // malformed entry — skip
                    }
                }

                System.out.println(String.format(
                        "[ItemSelector] %d total | badPrice=%d noSpread=%d cantAfford=%d manipulated=%d | candidates=%d",
                        total, badPrice, noSpread, cantAfford, manipulated, candidates.size()));

                if (candidates.isEmpty()) {
                    System.err.println("[ItemSelector] No viable items found.");
                    return null;
                }

                candidates.sort(Comparator.comparingDouble(s -> -s.score));

                System.out.println("[ItemSelector] Top 10 for purse " + (long) purse + ":");
                int rank = 1;
                for (ItemScore s : candidates.subList(0, Math.min(10, candidates.size()))) {
                    System.out.println(String.format(
                            "  #%d %s | spread=%.1f (%.2f%%) | weekly=%.0f/hr | totalProfit=%.0f | amount=%d",
                            rank++, s.displayName,
                            s.spread, s.spreadPercent * 100,
                            s.weeklyProfit, s.totalProfit, s.purchasableAmount));
                }

                ItemScore best = candidates.get(0);
                System.out.println("[ItemSelector] Selected: " + best.displayName);
                return best;

            } catch (Exception e) {
                System.err.println("[ItemSelector] Scoring failed: " + e.getMessage());
                return null;
            }
        });
    }

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
