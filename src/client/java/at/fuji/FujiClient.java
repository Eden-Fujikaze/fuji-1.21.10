package at.fuji;

import org.lwjgl.glfw.GLFW;
import at.rewrite.utils.WorldUtils;
import at.rewrite.mining.StateMachine;
import at.rewrite.utils.PlayerUtils;
import at.rewrite.utils.ScoreboardUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class FujiClient implements ClientModInitializer {
	public static final KeyBinding.Category FUJI_CATEGORY = KeyBinding.Category.create(Identifier.of("fuji", "fuji"));
	public static KeyBinding lookAtCobble;

	@Override
	public void onInitializeClient() {
		lookAtCobble = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fuji.look_at", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
				FUJI_CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (lookAtCobble.wasPressed()) {
				StateMachine.start();
			}
			StateMachine.tick();
		});
	}
}