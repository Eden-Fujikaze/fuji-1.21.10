package at.fuji;

import at.fuji.bazaar.BazaarWorker;
import at.fuji.bazaar.HypixelBazaarApi;
import at.fuji.bazaar.ItemSelector;
import at.fuji.render.*;
import at.fuji.target.*;
import at.fuji.ui.FujiScreen;

import org.lwjgl.glfw.GLFW;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class FujiClient implements ClientModInitializer {

	public static final KeyBinding.Category FUJI_CATEGORY = KeyBinding.Category
			.create(Identifier.of("fuji", "fuji"));

	public static KeyBinding openScreen;
	public static KeyBinding bazaarBot;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ModConfig.get().loadIntoTargetManager();

		HypixelBazaarApi.loadItemNames();

		openScreen = KeyBindingHelper.registerKeyBinding(
				new KeyBinding(
						"key.fuji.open_screen",
						InputUtil.Type.KEYSYM,
						GLFW.GLFW_KEY_G,
						FUJI_CATEGORY));

		bazaarBot = KeyBindingHelper.registerKeyBinding(
				new KeyBinding(
						"key.fuji.bazaar_bot",
						InputUtil.Type.KEYSYM,
						GLFW.GLFW_KEY_H,
						FUJI_CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null)
				return;
			BazaarWorker.update();
			TargetManager.refresh();
			if (openScreen.wasPressed()) {
				MinecraftClient.getInstance().setScreen(new FujiScreen());
			}
			if (bazaarBot.wasPressed()) {
				if (BazaarWorker.isEnabled()) {
					BazaarWorker.stop();
				} else {
					new BazaarWorker().start();
				}
			}
		});

		// HUD overlay
		HudElementRegistry.addLast(
				Identifier.of("fuji", "bazaar_status"),
				(drawContext, tickCounter) -> {

					MinecraftClient mc = MinecraftClient.getInstance();
					if (mc.world == null)
						return;
					String text = BazaarWorker.isEnabled() ? "Bazaar Bot: ON" : "Bazaar Bot: OFF";
					int color = BazaarWorker.isEnabled() ? 0xFF00FF00 : 0xFFFF0000;
					drawContext.drawTextWithShadow(mc.textRenderer, text, 5, 80, color);
				});

		// World render
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.world == null)
				return;

			Vec3d cam = mc.gameRenderer.getCamera().getPos();

			VertexConsumerProvider provider = context.consumers();

			// Sodium / fallback safety
			if (provider == null) {
				provider = mc.getBufferBuilders().getEntityVertexConsumers();
			}

			for (TargetConfig config : TargetManager.targets) {
				Vec3d pos = config.currentPos;
				if (pos == null)
					continue;

				if (config.waypointEnabled) {
					Waypoint.drawWaypoint(context.matrices(), pos, cam, provider);
				}

				if (config.tracerEnabled) {
					Tracer.drawTracer(context.matrices(), pos, cam, provider);
				}
			}

			// Flush if immediate provider
			if (provider instanceof VertexConsumerProvider.Immediate immediate) {
				immediate.draw();
			}
		});
	}
}