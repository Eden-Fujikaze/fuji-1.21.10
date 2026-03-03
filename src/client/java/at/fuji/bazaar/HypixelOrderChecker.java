package at.fuji.bazaar;

import at.fuji.ModConfig;
import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HypixelOrderChecker {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static class Order {
        public String productId;
        public String type; // "BUY" or "SELL"
        public int amount;
        public int filledAmount;
        public String status;

        public boolean isFilled() {
            return filledAmount >= amount;
        }
    }

    public static CompletableFuture<List<Order>> fetchOrders(String uuid) {
        String apiKey = ModConfig.get().apiKey;
        String url = "https://api.hypixel.net/v2/skyblock/profiles?uuid=" + uuid;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("API-Key", apiKey)
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    List<Order> orders = new ArrayList<>();
                    try {
                        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonArray profiles = root.getAsJsonArray("profiles");

                        // Find the most recently played profile
                        JsonObject latestProfile = null;
                        long latestTime = 0;
                        for (JsonElement pe : profiles) {
                            JsonObject profile = pe.getAsJsonObject();
                            long lastSave = profile.get("last_save").getAsLong();
                            if (lastSave > latestTime) {
                                latestTime = lastSave;
                                latestProfile = profile;
                            }
                        }

                        if (latestProfile == null)
                            return orders;

                        JsonObject members = latestProfile.getAsJsonObject("members");
                        JsonObject member = members.getAsJsonObject(uuid.replace("-", ""));
                        if (member == null)
                            return orders;

                        JsonArray bazaarOrders = member.getAsJsonArray("bazaar_orders");
                        if (bazaarOrders == null)
                            return orders;

                        for (JsonElement oe : bazaarOrders) {
                            JsonObject o = oe.getAsJsonObject();
                            Order order = new Order();
                            order.productId = o.get("product_id").getAsString();
                            order.type = o.get("type").getAsString();
                            order.amount = o.get("amount").getAsInt();
                            order.filledAmount = o.get("filled_amount").getAsInt();
                            order.status = o.get("status").getAsString();
                            orders.add(order);
                        }
                    } catch (Exception e) {
                        System.err.println("[HypixelOrderChecker] Parse error: " + e.getMessage());
                    }
                    return orders;
                });
    }
}