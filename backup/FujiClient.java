package at.fuji;

import at.fuji.render.*;
import at.fuji.target.*;
import at.fuji.ui.FujiScreen;
import at.fuji.utils.ScoreboardUtil;
import at.fuji.bazaar.*;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class FujiClient implements ClientModInitializer {

	public static final KeyMapping.Category FUJI_CATEGORY = KeyMapping.Category
			.register(ResourceLocation.fromNamespaceAndPath("fuji", "fuji"));

	public static KeyMapping openScreen;

	@Override
	public void onInitializeClient() {

		openScreen = KeyBindingHelper.registerKeyBinding(
				new KeyMapping(
						"key.fuji.open_screen",
						InputConstants.Type.KEYSYM,
						GLFW.GLFW_KEY_G,
						FUJI_CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null)
				return;
			TargetManager.refresh();
			if (BazaarCheck.checkInv()) {
				System.out.println("Player is in the Bazaar!");
			}

			if (openScreen.consumeClick()) {
				Minecraft.getInstance().setScreen(new FujiScreen());
			}
		});

		WorldRenderEvents.END_MAIN.register(context -> {
			if (context.consumers() == null)
				return;
			Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
			for (TargetConfig config : TargetManager.targets) {
				Vec3 pos = config.currentPos;
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