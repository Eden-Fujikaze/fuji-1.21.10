package at.rewrite.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class MenuUtils {
    private static SlotActionType PICKUP = net.minecraft.screen.slot.SlotActionType.PICKUP;

    private static ScreenHandler getHandler() {
        return MinecraftClient.getInstance().player.currentScreenHandler;
    }

    public static void clickSlot(int slotIndex, boolean primary) {
        ScreenHandler handler = getHandler();
        PlayerEntity player = GeneralUtils.getPlayer();
        if (slotIndex == -1) {
            System.out.println("[Fuji] Invalid slot index/Not found item");
        } else if (handler == null) {
            System.out.println("no screen");
        }

        if (primary) {
            MinecraftClient.getInstance().interactionManager.clickSlot(handler.syncId, slotIndex, 0, PICKUP, player);
        } else {
            MinecraftClient.getInstance().interactionManager.clickSlot(handler.syncId, slotIndex, 1, PICKUP, player);
        }
    }

    public static boolean isScreenOpen() {
        return MinecraftClient.getInstance().currentScreen != null;
    }

    public static boolean changedTitle(String titleBefore) {
        MinecraftClient client = GeneralUtils.getClient();
        if (client.currentScreen == null) {
            return false;
        }
        return !TextUtils.compareStrings(getCurrentScreenTitle(), titleBefore, true);

    }

    public static String getCurrentScreenTitle() {
        MinecraftClient client = GeneralUtils.getClient();
        if (client.currentScreen == null) {
            return null;
        }
        return client.currentScreen.getTitle().getString();

    }
}
