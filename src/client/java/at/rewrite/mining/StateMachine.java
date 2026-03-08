package at.rewrite.mining;

import at.rewrite.ConfigManager;
import at.rewrite.utils.GeneralUtils;
import at.rewrite.utils.PlayerUtils;
import at.rewrite.utils.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class StateMachine {

    public enum State {
        IDLE,
        FINDING_BLOCK,
        LOOKING_AT,
        MINING,
        DONE
    }

    private static State state = State.IDLE;
    private static BlockPos target;
    public static boolean enabled = false;
    private static final MinecraftClient client = GeneralUtils.getClient();

    /** Live block array — call reloadBlocks() after modifying the config list. */
    public static String[] blocks = ConfigManager.config.targetBlocks.toArray(new String[0]);

    // ── Control ───────────────────────────────────────────────────────────────

    /** Toggle the mining system on/off (keybind). */
    public static void start() {
        if (enabled) disable(); else enable();
    }

    /** Enable and reset state — call this when turning on from any context. */
    public static void enable() {
        enabled = true;
        state = State.IDLE;
        target = null;
        client.options.attackKey.setPressed(false);
    }

    /** Disable and clean up. */
    public static void disable() {
        enabled = false;
        state = State.IDLE;
        target = null;
        client.options.attackKey.setPressed(false);
    }

    /**
     * Reload the block array from config.
     * Call this whenever {@link at.rewrite.ModConfig#targetBlocks} changes.
     */
    public static void reloadBlocks() {
        blocks = ConfigManager.config.targetBlocks.toArray(new String[0]);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick() {
        if (!enabled) return;

        switch (state) {
            case IDLE -> state = State.FINDING_BLOCK;

            case FINDING_BLOCK -> {
                target = WorldUtils.findBlock(5, blocks);
                state = (target != null) ? State.LOOKING_AT : State.DONE;
            }

            case LOOKING_AT -> {
                if (isLookingAtTarget()) state = State.MINING;
            }

            case MINING -> {
                if (isLookingAtTarget()) {
                    client.options.attackKey.setPressed(true);
                } else {
                    client.options.attackKey.setPressed(false);
                    state = State.DONE;
                }
            }

            case DONE -> {
                target = null;
                state = State.IDLE;
                PlayerUtils.resetVelocity();
            }
        }
    }

    public static void tickCamera(float deltaTime) {
        if (!enabled) return;
        if (state == State.LOOKING_AT && target != null)
            PlayerUtils.lookAt(null, target, deltaTime);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isLookingAtTarget() {
        for (String block : blocks)
            if (PlayerUtils.isLookingAt(block)) return true;
        return false;
    }
}