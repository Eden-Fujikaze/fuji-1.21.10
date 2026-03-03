package at.fuji.bazaar;

/**
 * Scores a single bazaar item using:
 * weeklyProfit = (askPrice - bidPrice) * salesPerWeek / 168
 *
 * askPrice = quick_status.buyPrice (lowest ask — what you pay to place a buy
 * order)
 * bidPrice = quick_status.sellPrice (highest bid — what you get to
 * instant-sell)
 * spread = askPrice - bidPrice (profit per item under perfect conditions)
 */
public class ItemScore {
    public static final double HOURS_PER_WEEK = 168.0;
    private static final int MAX_ORDER_SIZE = 71_000;

    public final String productId;
    public final String displayName;

    // Raw API values (corrected naming)
    public final double askPrice; // what you pay (quick_status.buyPrice)
    public final double bidPrice; // what you get (quick_status.sellPrice)
    public final double buyMovingWeek;
    public final double sellMovingWeek;
    public final int buyOrders;
    public final int sellOrders;

    // Derived
    public final double spread; // askPrice - bidPrice
    public final double spreadPercent; // spread / askPrice
    public final double salesPerWeek; // bottleneck side items/week
    public final double weeklyProfit; // spread * salesPerWeek / 168 ← main metric
    public final int purchasableAmount;
    public final double totalProfit; // spread * purchasableAmount
    public final boolean manipulated;

    public final double score;

    public ItemScore(
            String productId, String displayName,
            double askPrice, double bidPrice,
            double buyMovingWeek, double sellMovingWeek,
            int buyOrders, int sellOrders,
            double purse) {
        this.productId = productId;
        this.displayName = displayName;
        this.askPrice = askPrice;
        this.bidPrice = bidPrice;
        this.buyMovingWeek = buyMovingWeek;
        this.sellMovingWeek = sellMovingWeek;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;

        this.spread = askPrice - bidPrice;
        this.spreadPercent = (askPrice > 0) ? spread / askPrice : 0;

        // salesPerWeek = bottleneck of both sides, converted to item count
        this.salesPerWeek = Math.min(buyMovingWeek, sellMovingWeek) / askPrice;

        // Core formula
        this.weeklyProfit = spread * salesPerWeek / HOURS_PER_WEEK;

        // How many can we afford, capped at Hypixel's 71k order limit
        int affordable = (askPrice > 0 && purse > 0)
                ? (int) Math.floor(purse / askPrice)
                : 0;
        this.purchasableAmount = Math.min(MAX_ORDER_SIZE, affordable);
        this.totalProfit = spread * purchasableAmount;

        // Manipulation detection
        boolean orderImbalance = buyOrders > 0 && sellOrders > 0
                && (double) Math.max(buyOrders, sellOrders)
                        / Math.min(buyOrders, sellOrders) > 20.0;
        boolean thinVolume = salesPerWeek < 1.0 && spread > 50_000;
        this.manipulated = orderImbalance || thinVolume;

        this.score = manipulated ? 0.0 : weeklyProfit;
    }

    @Override
    public String toString() {
        return String.format(
                "[%s%s] spread=%.1f (%.1f%%) weekly=%.0f/hr total=%.0f amount=%d",
                displayName, manipulated ? " ⚠MANIP" : "",
                spread, spreadPercent * 100,
                weeklyProfit, totalProfit, purchasableAmount);
    }
}