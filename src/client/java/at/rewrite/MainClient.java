package at.rewrite;

import at.rewrite.gui.screen.ConfigEditorScreen;
import at.rewrite.utils.GeneralUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import at.rewrite.mining.StateMachine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class MainClient implements ClientModInitializer {
    public static final KeyBinding.Category FUJI_CATEGORY = KeyBinding.Category.create(Identifier.of("fuji", "fuji"));
    public static KeyBinding lookAtCobble;
    public static KeyBinding openConfig;

    @Override
    public void onInitializeClient() {
        FujiConfig.HANDLER.load();
        StateMachine.blocks = FujiConfig.HANDLER.instance().blocks;

        lookAtCobble = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fuji.look_at", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                FUJI_CATEGORY));

        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fuji.open_cat", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8,
                FUJI_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.options != null) {
                mc.options.pauseOnLostFocus = false;
            }
            if (lookAtCobble.wasPressed()) {
                StateMachine.start();
            }
            if (openConfig.wasPressed()) {
                mc.setScreen(new ConfigEditorScreen());
            }
            StateMachine.tick();
        });

        WorldRenderEvents.END_MAIN.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            float deltaTime = mc.getRenderTickCounter().getDynamicDeltaTicks();
            StateMachine.tickCamera(deltaTime);
        });

        HudElementRegistry.addLast(Identifier.of("fuji", "bazaar_status"), (ctx, tick) -> {
           MinecraftClient mc = GeneralUtils.getClient();
           if (mc.world == null) return;
           String text = StateMachine.enabled ? "n-nya im mining mommy" : "grr im not mining or shi";
           int color = StateMachine.enabled ? 0xFF00FF00 : 0xFFFF0000;
           ctx.drawTextWithShadow(mc.textRenderer, text, 5, 80, color);
        });
    }
}