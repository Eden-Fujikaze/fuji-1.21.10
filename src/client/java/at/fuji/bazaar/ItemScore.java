package at.fuji.bazaar;

/**
 * Holds the scored metrics for a single bazaar item.
 * All values are pre-calculated from a single API snapshot.
 */
public class ItemScore {
    public final String productId;
    public final String displayName;

    // Raw API values
    public final double buyPrice;       // what you pay to place a buy order
    public final double sellPrice;      // what you get when placing a sell order
    public final double buyMovingWeek;  // total coins of buy orders placed this week
    public final double sellMovingWeek; // total coins of sell orders placed this week
    public final int    buyOrders;      // number of open competing buy orders

    // Derived values
    public final double profitPerItem;     // sellPrice - buyPrice
    public final double profitMargin;      // profitPerItem / buyPrice  (relative margin)
    public final int    purchasableAmount; // how many you can afford, capped by sell supply
    public final double totalProfit;       // profitPerItem * purchasableAmount
    public final double fillSpeed;         // proxy: sellMovingWeek / buyOrders (higher = fills faster)

    // Final composite score
    public final double score;

    public ItemScore(
            String productId,
            String displayName,
            double buyPrice,
            double sellPrice,
            double buyMovingWeek,
            double sellMovingWeek,
            int    buyOrders,
            double purse
    ) {
        this.productId      = productId;
        this.displayName    = displayName;
        this.buyPrice       = buyPrice;
        this.sellPrice      = sellPrice;
        this.buyMovingWeek  = buyMovingWeek;
        this.sellMovingWeek = sellMovingWeek;
        this.buyOrders      = buyOrders;

        // How many can we afford? Cap at realistic hourly sell supply.
        // We place a buy order at ~sellPrice, so we care about sell-side volume (buyMovingWeek
        // tracks coins spent on buy orders — sellMovingWeek tracks coins from sell orders).
        // sellMovingWeek / sellPrice / 7 / 24 = average items sold per hour
        int affordable = (sellPrice > 0 && purse > 0)
                ? (int) Math.floor(purse / sellPrice)  // we order at sellPrice + 0.1
                : 0;
        int hourlySupply = (int) (sellMovingWeek / sellPrice / 7.0 / 24.0);
        this.purchasableAmount = Math.min(affordable, Math.max(1, hourlySupply));

        // buyPrice = lowest ask (instant buy cost)
        // sellPrice = highest bid (instant sell return)
        // Flip strategy: place buy order just above sellPrice, sell order just below buyPrice
        // The spread is what you pocket: buyPrice - sellPrice
        this.profitPerItem  = buyPrice - sellPrice;
        this.profitMargin   = (buyPrice > 0) ? profitPerItem / buyPrice : 0;
        this.totalProfit    = profitPerItem * purchasableAmount;

        // Fill speed: sell-side weekly volume per competing buy order.
        // More sell volume + fewer open buy orders = your order fills faster.
        this.fillSpeed = (buyOrders > 0) ? sellMovingWeek / buyOrders : sellMovingWeek;

        // Composite score — weight each factor.
        // We normalise totalProfit by purse so large-purse players don't
        // purely chase the most expensive item.
        double normalisedProfit = (purse > 0) ? totalProfit / purse : 0;

        this.score = (normalisedProfit   * 0.40)  // 40% — are we actually making money?
                   + (profitMargin       * 0.25)  // 25% — margin protects against price swings
                   + (fillSpeed / 1e9    * 0.20)  // 20% — will this fill quickly? (scaled down)
                   + (profitPerItem / 1e4 * 0.15); // 15% — raw coins per item (scaled down)
    }

    @Override
    public String toString() {
        return String.format(
            "[%s] score=%.4f profit/item=%.1f total=%.0f amount=%d fillSpeed=%.0f margin=%.2f%%",
            displayName, score, profitPerItem, totalProfit, purchasableAmount,
            fillSpeed, profitMargin * 100
        );
    }
}