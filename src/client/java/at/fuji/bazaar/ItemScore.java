package at.fuji.bazaar;

public class ItemScore {
        public static final double HOURS_PER_WEEK = 168.0;
        private static final int MAX_ORDER_SIZE = 71_000;

        public final String productId;
        public final String displayName;

        public final double askPrice;
        public final double bidPrice;
        public final double npcSellPrice;
        public final double buyMovingWeek;
        public final double sellMovingWeek;
        public final int buyOrders;
        public final int sellOrders;

        public final double spread;
        public final double spreadPercent;
        public final double fillRatePerHour; // items/hr on slowest side
        public final double weeklyProfit; // profit per hour
        public final int purchasableAmount;
        public final double totalProfit;
        public final boolean manipulated;
        public final double score;

        public final double npcProfitPerItem;
        public final double npcProfitPerHour;

        public ItemScore(String productId, String displayName,
                        double askPrice, double bidPrice,
                        double buyMovingWeek, double sellMovingWeek,
                        int buyOrders, int sellOrders,
                        double purse, double npcSellPrice) {
                this.productId = productId;
                this.displayName = displayName;
                this.askPrice = askPrice;
                this.bidPrice = bidPrice;
                this.npcSellPrice = npcSellPrice;
                this.buyMovingWeek = buyMovingWeek;
                this.sellMovingWeek = sellMovingWeek;
                this.buyOrders = buyOrders;
                this.sellOrders = sellOrders;

                this.spread = askPrice - bidPrice;
                this.spreadPercent = (askPrice > 0) ? spread / askPrice : 0;

                double salesPerWeek = Math.min(buyMovingWeek, sellMovingWeek) / Math.max(askPrice, 1);
                this.fillRatePerHour = salesPerWeek / HOURS_PER_WEEK;
                this.weeklyProfit = spread * fillRatePerHour;

                int affordable = (askPrice > 0 && purse > 0) ? (int) Math.floor(purse / askPrice) : 0;
                this.purchasableAmount = Math.min(MAX_ORDER_SIZE, affordable);
                this.totalProfit = spread * purchasableAmount;

                boolean orderImbalance = buyOrders > 0 && sellOrders > 0
                                && (double) Math.max(buyOrders, sellOrders) / Math.min(buyOrders, sellOrders) > 20.0;
                boolean thinVolume = fillRatePerHour < 1.0 && spread > 50_000;
                this.manipulated = orderImbalance || thinVolume;
                this.score = manipulated ? 0.0 : weeklyProfit;

                this.npcProfitPerItem = Math.max(0, npcSellPrice - askPrice);
                double npcFillRate = buyMovingWeek / Math.max(askPrice, 1) / HOURS_PER_WEEK;
                this.npcProfitPerHour = (npcSellPrice > askPrice) ? npcProfitPerItem * npcFillRate : 0;
        }

        @Override
        public String toString() {
                return String.format("[%s%s] spread=%.1f (%.1f%%) profit/hr=%.0f total=%.0f amount=%d",
                                displayName, manipulated ? " ⚠" : "",
                                spread, spreadPercent * 100, weeklyProfit, totalProfit, purchasableAmount);
        }
}