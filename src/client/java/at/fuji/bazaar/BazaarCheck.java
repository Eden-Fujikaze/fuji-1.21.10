package at.fuji.bazaar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class BazaarCheck {
    public static boolean checkInv() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen != null && screen.getTitle().getString().contains("Bazaar");
    }
}
