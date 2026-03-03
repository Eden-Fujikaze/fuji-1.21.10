package at.fuji.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;

public class BazaarUtils {

    public static class FilledOrder {
        public String itemName;
        public int amount;

        public FilledOrder(String itemName, int amount) {
            this.itemName = itemName;
            this.amount = amount;
        }
    }

    public static FilledOrder getFilledBuyOrder() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return null;
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null)
            return null;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot == null || !slot.hasStack())
                continue;
            ItemStack stack = slot.getStack();
            if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                continue;

            String slotName = LoreUtils.getSlotName(stack);
            if (!slotName.startsWith("BUY "))
                continue;
            if (!isOrderFilled(stack))
                continue;

            int amount = LoreUtils.parseAmount(stack, "Order amount:");
            if (amount <= 0)
                continue;

            return new FilledOrder(slotName.replace("BUY ", "").trim(), amount);
        }
        return null;
    }

    public static FilledOrder getFilledSellOrder() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return null;
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null)
            return null;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot == null || !slot.hasStack())
                continue;
            ItemStack stack = slot.getStack();
            if (stack.getItem() == net.minecraft.item.Items.RED_STAINED_GLASS_PANE)
                continue;

            String slotName = LoreUtils.getSlotName(stack);
            if (!slotName.startsWith("SELL "))
                continue;
            if (!isOrderFilled(stack))
                continue;

            int amount = LoreUtils.parseAmount(stack, "Offer amount:");
            if (amount <= 0)
                continue;

            return new FilledOrder(slotName.replace("SELL ", "").trim(), amount);
        }
        return null;
    }

    public static boolean isOrderFilled(ItemStack stack) {
        return LoreUtils.loreContains(stack, "100%");
    }

    public static String extractItemName(ItemStack stack) {
        String slotName = LoreUtils.getSlotName(stack);
        if (slotName.startsWith("BUY "))
            return slotName.replace("BUY ", "").trim();
        if (slotName.startsWith("SELL "))
            return slotName.replace("SELL ", "").trim();
        return slotName;
    }
}