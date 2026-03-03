package at.fuji.utils;

import net.minecraft.client.MinecraftClient;

public class InputUtils {
    public static void typeIntoScreen(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null)
            return;
        for (char c : text.toCharArray()) {
            net.minecraft.client.input.CharInput input = new net.minecraft.client.input.CharInput(c, 0);
            client.currentScreen.charTyped(input);
        }
    }

    public static void pressEnter() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null)
            return;
        client.currentScreen.keyPressed(
                new net.minecraft.client.input.KeyInput(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0));
    }

    public static void pressEscape() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null)
            return;
        client.currentScreen.keyPressed(
                new net.minecraft.client.input.KeyInput(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0));
    }

    public static boolean isInputScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null)
            return false;
        return client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.AnvilScreen
                || client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.SignEditScreen;
    }

    public static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;
        client.player.networkHandler.sendChatCommand(command);
    }
}
