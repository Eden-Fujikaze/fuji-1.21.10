package at.fuji.bazaar;

import com.google.gson.JsonObject;

import at.fuji.ModConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

/**
 * Scores every Hypixel Bazaar item and picks the best one to flip (or
 * NPC-sell, when {@link ModConfig#npcSellMode} is enabled).
 *
 * <h3>Flip mode (default)</h3>
 * 
 * <pre>
 * profit/item  = askPrice - bidPrice          (spread)
 * salesPerWeek = min(buyMovingWeek, sellMovingWeek) / askPrice
 * score        = profit/item * salesPerWeek / HOURS_PER_WEEK
 * </pre>
 *
 * <h3>NPC sell mode</h3>
 * Only items where {@code npcSellPrice > askPrice} are considered.
 * 
 * <pre>
 * profit/item  = npcSellPrice - askPrice
 * salesPerWeek = buyMovingWeek / askPrice      (only buy side matters)
 * score        = profit/item * salesPerWeek / HOURS_PER_WEEK
 * </pre>
 *
 * <p>
 * API field semantics (confusingly named by Hypixel):
 * <ul>
 * <li>{@code quick_status.buyPrice} = lowest ask — what you PAY to
 * instant-buy</li>
 * <li>{@code quick_status.sellPrice} = highest bid — what you GET to
 * instant-sell</li>
 * <li>{@code npc_sell_price} (items endpoint) = fixed NPC sell value (0 if not
 * NPC-sellable)</li>
 * </ul>
 */
public class ItemSelector {

    public static CompletableFuture<ItemScore> selectBestItem(double purse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject allProducts = HypixelBazaarApi.getAllProductsSync();
                if (allProducts == null) {
                    System.err.println("[ItemSelector] Could not fetch bazaar data.");
                    return null;
                }

                // Read fresh every call — avoids stale reference if ModConfig reloads
                List<String> blacklist = ModConfig.get().bazaarBlacklist;
                boolean npcMode = ModConfig.get().npcSellMode;

                List<ItemScore> candidates = new ArrayList<>();
                Map<ItemScore, Double> effectiveScores = new HashMap<>();
                int total = 0, badPrice = 0, noSpread = 0, cantAfford = 0,
                        manipulated = 0, noNpc = 0;

                for (String productId : allProducts.keySet()) {
                    if (blacklist.contains(productId))
                        continue;
                    if (productId.startsWith("ESSENCE"))
                        continue;

                    total++;

                    try {
                        JsonObject qs = allProducts
                                .getAsJsonObject(productId)
                                .getAsJsonObject("quick_status");

                        // API naming: buyPrice = ask (you pay), sellPrice = bid (you receive)
                        double askPrice = qs.get("buyPrice").getAsDouble();
                        double bidPrice = qs.get("sellPrice").getAsDouble();
                        double buyMovingWeek = qs.get("buyMovingWeek").getAsDouble();
                        double sellMovingWeek = qs.get("sellMovingWeek").getAsDouble();
                        int buyOrders = qs.get("buyOrders").getAsInt();
                        int sellOrders = qs.get("sellOrders").getAsInt();
                        // NPC sell price lives in the items endpoint, not bazaar quick_status
                        double npcSellPrice = HypixelBazaarApi.getNpcSellPriceSync(productId);

                        if (askPrice <= 0 || bidPrice <= 0) {
                            badPrice++;
                            continue;
                        }
                        if (purse < askPrice) {
                            cantAfford++;
                            continue;
                        }

                        double profitPerItem;
                        double salesPerWeek;

                        if (npcMode) {
                            // ── NPC sell path ──────────────────────────────
                            // Only viable if NPC pays more than buy-order cost
                            if (npcSellPrice <= askPrice) {
                                noNpc++;
                                continue;
                            }
                            profitPerItem = npcSellPrice - askPrice;
                            // Volume: how many items move through buy orders per week
                            salesPerWeek = buyMovingWeek / askPrice;

                        } else {
                            // ── Flip path ──────────────────────────────────
                            if (askPrice <= bidPrice) {
                                noSpread++;
                                continue;
                            }
                            // Minimum absolute profit filter (avoids micro-spread items)
                            if (askPrice - bidPrice < 50) {
                                noSpread++;
                                continue;
                            }
                            // Minimum relative spread (avoid items where tax eats profit)
                            double spreadPct = (askPrice - bidPrice) / askPrice;
                            if (spreadPct < 0.02) { // less than 2 %
                                noSpread++;
                                continue;
                            }
                            profitPerItem = askPrice - bidPrice;
                            salesPerWeek = Math.min(buyMovingWeek, sellMovingWeek) / askPrice;
                        }

                        double weeklyProfit = profitPerItem * salesPerWeek / ItemScore.HOURS_PER_WEEK;

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

                        double effectiveScore = npcMode
                                ? profitPerItem * salesPerWeek / ItemScore.HOURS_PER_WEEK
                                : score.score;
                        effectiveScores.put(score, effectiveScore);
                        candidates.add(score);

                    } catch (Exception e) {
                        // malformed entry — skip
                    }
                }

                System.out.printf(
                        "[ItemSelector] %s | %d total | badPrice=%d noSpread=%d noNpc=%d cantAfford=%d manipulated=%d | candidates=%d%n",
                        npcMode ? "NPC-SELL" : "FLIP",
                        total, badPrice, noSpread, noNpc, cantAfford, manipulated, candidates.size());

                if (candidates.isEmpty()) {
                    System.err.println("[ItemSelector] No viable items found.");
                    return null;
                }

                candidates.sort(Comparator.comparingDouble(s -> -effectiveScores.getOrDefault(s, s.score)));

                System.out.println("[ItemSelector] Top 10 for purse " + (long) purse
                        + (npcMode ? " [NPC mode]" : " [flip mode]") + ":");
                int rank = 1;
                for (ItemScore s : candidates.subList(0, Math.min(10, candidates.size()))) {
                    System.out.printf(
                            "  #%d %s | spread=%.1f (%.2f%%) | weekly=%.0f/hr | totalProfit=%.0f | amount=%d%n",
                            rank++, s.displayName,
                            s.spread, s.spreadPercent * 100,
                            s.weeklyProfit, s.totalProfit, s.purchasableAmount);
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