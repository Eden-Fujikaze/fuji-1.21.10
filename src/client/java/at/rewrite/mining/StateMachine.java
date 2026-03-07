package at.rewrite.mining;

import at.rewrite.utils.GeneralUtils;
import at.rewrite.utils.PlayerUtils;
import at.rewrite.utils.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
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
    public static String[] blocks = new String[]{};
    private static final MinecraftClient client = GeneralUtils.getClient();

    public static void start() {
        enabled = !enabled;
        if (enabled) {
            state = State.IDLE;
            target = null;
        }
    }

    public static void tick() {
        if (!enabled) return;

        switch (state) {
            case IDLE -> state = State.FINDING_BLOCK;

            case FINDING_BLOCK -> {
                assert client.player != null;
                double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
                target = WorldUtils.findBlock((int) reach, blocks);
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
            }
        }
    }

    public static void tickCamera(float deltaTime) {
        if (!enabled) return;
        if (state == State.LOOKING_AT && target != null) {
            PlayerUtils.lookAt(null, target, deltaTime);
        }
    }

    private static boolean isLookingAtTarget() {
        for (String block : blocks) {
            if (PlayerUtils.isLookingAt(block)) return true;
        }
        return false;
    }
}