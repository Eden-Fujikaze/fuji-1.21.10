package at.fuji.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class MenuUtils {
    static ScreenHandler handler;

    public static int scanForSlot(String name, boolean exact) {
        ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
        String stripped = StripUtils.stripColorCodes(name);
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot == null || !slot.hasStack())
                continue;
            String slotName = StripUtils.stripColorCodes(slot.getStack().getName().getString());
            if (exact) {
                if (slotName.equals(stripped))
                    return i;
            } else {
                if (slotName.contains(stripped))
                    return i;
            }
        }
        return -1;
    }

    public static int scanForSlotWithLore(String loreText) {
        return -1;
    }

    public static int scanForSlotStartingWith(String prefix) {
        return -1;
    }

    public static void clickSlot(int index, boolean primary) {
        handler = MinecraftClient.getInstance().player.currentScreenHandler;
        if (index == -1 || handler == null) {
            return;
        }
        if (primary) {
            MinecraftClient.getInstance().interactionManager.clickSlot(handler.syncId, index, 0,
                    net.minecraft.screen.slot.SlotActionType.PICKUP, MinecraftClient.getInstance().player);
        } else {
            MinecraftClient.getInstance().interactionManager.clickSlot(handler.syncId, index, 1,
                    net.minecraft.screen.slot.SlotActionType.PICKUP, MinecraftClient.getInstance().player);
        }
    }

    public static boolean isScreenOpen() {
        return MinecraftClient.getInstance().currentScreen != null;
    }

    public static boolean screenTitleChanged(String titleBefore) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null)
            return false;
        return !client.currentScreen.getTitle().getString().equals(titleBefore);
    }

    public static String getCurrentScreenTitle() {
        if (MinecraftClient.getInstance().currentScreen == null)
            return null;
        return MinecraftClient.getInstance().currentScreen.getTitle().getString();
    }
}
