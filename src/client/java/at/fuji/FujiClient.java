package at.fuji;

import at.fuji.bazaar.BazaarWorker;
import at.fuji.bazaar.BazaarChatListener;
import at.fuji.render.*;
import at.fuji.target.*;
import at.fuji.ui.FujiScreen;

import org.lwjgl.glfw.GLFW;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
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

		BazaarChatListener.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null)
				return;
			BazaarWorker.update();
			TargetManager.refresh();
			if (openScreen.isPressed()) {
				MinecraftClient.getInstance().setScreen(new FujiScreen());
			}
			if (bazaarBot.wasPressed()) {
				new BazaarWorker().start();
			}
		});

		WorldRenderEvents.END_MAIN.register(context -> {
			if (context.consumers() == null)
				return;
			Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
			for (TargetConfig config : TargetManager.targets) {
				Vec3d pos = config.currentPos;
				if (pos == null)
					continue;

				if (config.waypointEnabled) {
					Waypoint.drawWaypoint(context.matrices(), pos, cam, context.consumers());
				}
				if (config.tracerEnabled) {
					Tracer.drawTracer(context.matrices(), pos, cam, context.consumers());
				}
			}
		});
	}
}