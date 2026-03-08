package at.rewrite;

import at.rewrite.gui.dsl.ConfigScreen;
import at.rewrite.gui.dsl.Option;
import at.rewrite.gui.dsl.Tab;
import at.rewrite.mining.StateMachine;
import at.rewrite.mob.AutoHit;
import at.rewrite.utils.PlayerUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MainClient implements ClientModInitializer {

    public static final KeyBinding.Category REWRITE_CATEGORY =
            KeyBinding.Category.create(Identifier.of("rewrite", "rewrite"));

    /** G — toggle mining on/off */
    public static KeyBinding miningToggle;

    /** H — open the config GUI */
    public static KeyBinding openConfig;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        miningToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rewrite.mining_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                REWRITE_CATEGORY));

        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rewrite.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                REWRITE_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (miningToggle.wasPressed())
                StateMachine.start();

            if (openConfig.wasPressed()) openConfigScreen();
            StateMachine.tick();
        });

        WorldRenderEvents.END_MAIN.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            float deltaTime = mc.getRenderTickCounter().getDynamicDeltaTicks();
            StateMachine.tickCamera(deltaTime);
            if (AutoHit.enabled) {
                AutoHit.tick(deltaTime, 4.5F);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // This is the ONLY place you ever define the UI.
    // No pixel math. No layout code. Just describe what you want.
    // ─────────────────────────────────────────────────────────────────────────

    private static void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(new ConfigScreen("Rewrite Config",
                Tab.named("Camera").add(
                        new Option.NumInput(
                                "Smoothing",
                                () -> ConfigManager.config.smoothing,
                                v -> { ConfigManager.config.smoothing = v; ConfigManager.save(); },
                                0.5f,
                                1f,
                                30f
                        ),
                        new Option.NumInput(
                                "Max Speed",
                                () -> ConfigManager.config.maxVelocity,
                                v -> { ConfigManager.config.maxVelocity = v; ConfigManager.save(); },
                                0.5f,
                                1f,
                                30f
                        ),
                        new Option.NumInput(
                                "Threshold",
                                () -> ConfigManager.config.threshold,
                                v -> { ConfigManager.config.threshold = v; ConfigManager.save(); },
                                0.1f,
                                0f,
                                5f
                        ),
                        new Option.NumInput(
                                "Aim Thresh",
                                () -> ConfigManager.config.aimSlop,
                                v -> { ConfigManager.config.aimSlop = v; ConfigManager.save(); },
                                0.5f,
                                0f,
                                15f
                        ),
                        new Option.NumInput(
                                "Overshoot",
                                () -> ConfigManager.config.overshoot,
                                v -> { ConfigManager.config.overshoot = v; ConfigManager.save(); },
                                0.5f,
                                0f,
                                15f
                        )
                ),
                Tab.named("Targets").add(
                        new Option.Label("Hit nearby enemies"),
                        new Option.Toggle(
                                "Enabled",
                                () -> AutoHit.enabled,
                                v -> { if (v) AutoHit.enable(); else AutoHit.disable(); }
                        ),
                        new Option.Spacer(8),
                        new Option.Label("Target Entities"),
                        new Option.EntryList(
                                "Entities to hit",
                                () -> ConfigManager.config.targetEntities,
                                list -> {
                                    AutoHit.reload();
                                    ConfigManager.save();
                                }
                        )
                ),
                Tab.named("Mining").add(
                        new Option.Label("Auto Mine"),
                        new Option.Toggle(
                                "Enabled",
                                () -> StateMachine.enabled,
                                v  -> { if (v) StateMachine.enable(); else StateMachine.disable(); }
                        ),
                        new Option.Hotkey(
                                "Toggle Key",
                                () -> miningToggle.getBoundKeyLocalizedText().getString(),
                                ()  -> { /* start rebind flow here if you want */ }
                        ),
                        new Option.Spacer(8),
                        new Option.Label("Target Blocks"),
                        new Option.EntryList(
                                "Blocks to mine",
                                () -> ConfigManager.config.targetBlocks,
                                list -> {
                                    StateMachine.reloadBlocks();
                                    ConfigManager.save();
                                }
                        )
                ),

                Tab.named("General").add(
                        new Option.Toggle(
                                "Debug Mode",
                                () -> false,   // replace with your own config field
                                v  -> {}
                        )
                )

        ));
    }
}