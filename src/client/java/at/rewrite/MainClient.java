package at.rewrite;

import org.lwjgl.glfw.GLFW;

import at.rewrite.ConfigManager;
import at.rewrite.mining.StateMachine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class MainClient implements ClientModInitializer {
    public static final KeyBinding.Category FUJI_CATEGORY = KeyBinding.Category.create(Identifier.of("fuji", "fuji"));
    public static KeyBinding lookAtCobble;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        lookAtCobble = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fuji.look_at", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                FUJI_CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (lookAtCobble.wasPressed()) {
                StateMachine.start();
            }
            StateMachine.tick();
        });

        WorldRenderEvents.END_MAIN.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null)
                return;
            float deltaTime = mc.getRenderTickCounter().getDynamicDeltaTicks();
            StateMachine.tickCamera(deltaTime);
        });
    }
}