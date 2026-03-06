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
    private static MinecraftClient client = GeneralUtils.getClient();
    public static String[] blocks = ConfigManager.config.targetBlocks.toArray(new String[0]);

    public static void start() {
        enabled = !enabled;
        if (enabled) {
            state = State.IDLE;
            target = null;
        }
    }

    public static void tick() {
        if (!enabled)
            return;

        switch (state) {
            case IDLE -> {
                state = State.FINDING_BLOCK;
            }

            case FINDING_BLOCK -> {
                target = WorldUtils.findBlock(5, blocks);
                if (target != null) {
                    state = State.LOOKING_AT;
                } else {
                    state = State.DONE;
                }
            }

            case LOOKING_AT -> {
                if (isLookingAtTarget()) {
                    state = State.MINING;
                }
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
        if (!enabled)
            return;
        if (state == State.LOOKING_AT && target != null) {
            PlayerUtils.lookAt(null, target, deltaTime);
        }
    }

    private static boolean isLookingAtTarget() {
        for (String block : blocks) {
            if (PlayerUtils.isLookingAt(block)) {
                return true;
            }
        }
        return false;
    }
}