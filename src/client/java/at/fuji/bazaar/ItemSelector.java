package at.fuji.bazaar;

import at.fuji.ModConfig;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ItemSelector {

    // ── Result types ──────────────────────────────────────────────────────────

    public static class RejectedItem {
        public final String productId;
        public final String displayName;
        public final double askPrice;
        public final double bidPrice;
        public final String reason;

        public RejectedItem(String productId, String displayName,
                double askPrice, double bidPrice, String reason) {
            this.productId = productId;
            this.displayName = displayName;
            this.askPrice = askPrice;
            this.bidPrice = bidPrice;
            this.reason = reason;
        }
    }

    public static class ScoringResult {
        public final List<ItemScore> candidates;
        public final List<RejectedItem> rejected;

        public ScoringResult(List<ItemScore> candidates, List<RejectedItem> rejected) {
            this.candidates = candidates;
            this.rejected = rejected;
        }

        public ItemScore best() {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    public static volatile ScoringResult lastResult = null;
    public static volatile ItemScore lastBest = null;

    // ── Public API ────────────────────────────────────────────────────────────

    /** Convenience wrapper — picks the highest-scoring item. */
    public static CompletableFuture<ItemScore> selectBestItem(double purse) {
        return scoreAllItems(purse).thenApply(r -> {
            if (r == null || r.candidates.isEmpty())
                return null;
            return r.candidates.get(0);
        });
    }

    public static CompletableFuture<ScoringResult> scoreAllItems(double purse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject allProducts = HypixelBazaarApi.getAllProductsSync();
                if (allProducts == null)
                    return null;

                ModConfig cfg = ModConfig.get();
                List<String> blacklist = cfg.bazaarBlacklist;
                boolean npcMode = cfg.npcSellMode;

                List<ItemScore> candidates = new ArrayList<>();
                List<RejectedItem> rejected = new ArrayList<>();

                for (String productId : allProducts.keySet()) {
                    if (blacklist.contains(productId))
                        continue;
                    if (productId.startsWith("ESSENCE"))
                        continue;

                    String displayName = HypixelBazaarApi.getItemName(productId);
                    double askPrice = -1, bidPrice = -1;

                    try {
                        JsonObject qs = allProducts
                                .getAsJsonObject(productId)
                                .getAsJsonObject("quick_status");

                        askPrice = qs.get("buyPrice").getAsDouble();
                        bidPrice = qs.get("sellPrice").getAsDouble();
                        double buyVol = qs.get("buyMovingWeek").getAsDouble();
                        double sellVol = qs.get("sellMovingWeek").getAsDouble();
                        int buyOrd = qs.get("buyOrders").getAsInt();
                        int sellOrd = qs.get("sellOrders").getAsInt();
                        double npcP = HypixelBazaarApi.getNpcSellPriceSync(productId);

                        // ── Hard validity ──────────────────────────────────
                        if (askPrice <= 0 || bidPrice <= 0) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Invalid price"));
                            continue;
                        }
                        // Price floor — eliminates <500-coin garbage
                        if (askPrice < 200) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Price floor (<500)"));
                            continue;
                        }
                        if (purse < askPrice) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Can't afford"));
                            continue;
                        }

                        // ── Sell-orders filter ─────────────────────────────
                        if (cfg.minSellOrders > 0 && sellOrd < cfg.minSellOrders) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Sell orders " + sellOrd + " < " + cfg.minSellOrders));
                            continue;
                        }

                        double profitPerItem, salesPerWeek;

                        if (npcMode) {
                            if (npcP <= askPrice) {
                                rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                        "NPC " + fmt(npcP) + " \u2264 ask"));
                                continue;
                            }
                            profitPerItem = npcP - askPrice;
                            salesPerWeek = buyVol / askPrice;
                        } else {
                            double spread = askPrice - bidPrice;
                            if (spread <= 0) {
                                rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                        "No spread"));
                                continue;
                            }
                            // Configurable absolute margin
                            if (spread < cfg.minMargin) {
                                rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                        "Margin " + fmt(spread) + " < " + fmt(cfg.minMargin)));
                                continue;
                            }
                            // Relative floor: ≥1% spread to survive 1% Hypixel tax
                            if (spread / askPrice < 0.01) {
                                rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                        "Spread% <1%"));
                                continue;
                            }
                            profitPerItem = spread;
                            salesPerWeek = Math.min(buyVol, sellVol) / askPrice;
                        }

                        // ── Volume filter ──────────────────────────────────
                        double slowVol = Math.min(buyVol, sellVol);
                        if (slowVol < cfg.minWeeklyVolume) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Vol " + fmt(slowVol) + " < " + fmt(cfg.minWeeklyVolume)));
                            continue;
                        }

                        double fillRatePerHour = salesPerWeek / ItemScore.HOURS_PER_WEEK;
                        double profitPerHour = profitPerItem * fillRatePerHour;

                        if (profitPerHour <= 0)
                            continue;

                        // ── Fill-rate filter ───────────────────────────────
                        if (fillRatePerHour < cfg.minSellsPerHour) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Fill " + (int) fillRatePerHour + "/hr < " + cfg.minSellsPerHour));
                            continue;
                        }

                        // ── Profit/hr filter (togglable) ───────────────────
                        if (cfg.minProfitPerHourEnabled && profitPerHour < cfg.minProfitPerHour) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "P/hr " + fmt(profitPerHour) + " < " + fmt(cfg.minProfitPerHour)));
                            continue;
                        }

                        ItemScore score = new ItemScore(
                                productId, displayName,
                                askPrice, bidPrice,
                                buyVol, sellVol,
                                buyOrd, sellOrd,
                                purse, npcP);

                        if (score.purchasableAmount < 1) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Can't afford any"));
                            continue;
                        }

                        // ── Manipulation ───────────────────────────────────
                        boolean orderImbalance = buyOrd > 0 && sellOrd > 0
                                && (double) Math.max(buyOrd, sellOrd) / Math.min(buyOrd, sellOrd) > 20.0;
                        boolean thinVolume = fillRatePerHour < 1.0 && profitPerItem > 50_000;
                        if (orderImbalance || thinVolume) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    orderImbalance ? "Order imbalance" : "Thin volume"));
                            continue;
                        }

                        // ── Total-profit filter (togglable) ────────────────
                        if (cfg.minTotalProfitEnabled && score.totalProfit < cfg.minTotalProfit) {
                            rejected.add(rej(productId, displayName, askPrice, bidPrice,
                                    "Total " + fmt(score.totalProfit) + " < " + fmt(cfg.minTotalProfit)));
                            continue;
                        }

                        candidates.add(score);

                    } catch (Exception e) {
                        rejected.add(rej(productId, displayName, askPrice, bidPrice, "Parse error"));
                    }
                }

                // Default sort: profit/hr desc
                candidates.sort(Comparator.comparingDouble(s -> -s.weeklyProfit));

                System.out.printf("[ItemSelector] %s | candidates=%d rejected=%d purse=%.0f%n",
                        npcMode ? "NPC" : "FLIP", candidates.size(), rejected.size(), purse);
                if (!candidates.isEmpty())
                    System.out.println("[ItemSelector] #1: " + candidates.get(0));

                ScoringResult result = new ScoringResult(candidates, rejected);
                lastResult = result;
                lastBest = result.best();
                return result;

            } catch (Exception e) {
                System.err.println("[ItemSelector] Failed: " + e.getMessage());
                return null;
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static RejectedItem rej(String id, String name, double ask, double bid, String reason) {
        return new RejectedItem(id, name, ask, bid, reason);
    }

    private static String fmt(double v) {
        if (v >= 1_000_000)
            return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000)
            return String.format("%.0fK", v / 1_000);
        return String.format("%.0f", v);
    }
}