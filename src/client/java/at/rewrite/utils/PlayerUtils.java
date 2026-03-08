package at.rewrite.utils;

import at.rewrite.ConfigManager;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class PlayerUtils {

    private static float yawVelocity   = 0f;
    private static float pitchVelocity = 0f;

    // Per-acquisition randomised aim state
    private static Vec3d  lastTargetBase   = null;
    private static float  aimOffsetYaw     = 0f;
    private static float  aimOffsetPitch   = 0f;
    private static float  thisSmoothing    = 8f;   // varies each acquisition
    private static float  reactionTimer    = 0f;   // delay before moving
    private static boolean hasOvershot     = false;
    private static float  overshootAmount  = 0f;

    private static final Random random = new Random();

    private static float wrapAngle(float a) {
        return ((a % 360f) + 360f) % 360f;
    }

    private static float angleDiff(float current, float target) {
        float diff = wrapAngle(target) - wrapAngle(current);
        return ((diff + 540f) % 360f) - 180f;
    }

    private static float[] getAnglesTo(Vec3d target, Vec3d eyes) {
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double h  = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(MathHelper.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(MathHelper.atan2(dy, h));
        return new float[]{ yaw, pitch };
    }

    // Critically damped spring step — returns [angleDelta, newVelocity]
    private static float[] springStep(float current, float goal, float vel,
                                      float stiffness, float dt) {
        float omega = 2f * stiffness;
        float x     = omega * dt;
        float exp   = (float) Math.exp(-x);
        float delta = angleDiff(current, goal);
        float newVel   = (vel + omega * delta) * exp - omega * delta * exp;
        float newDelta = delta * (1f - exp) + vel * dt * exp;
        return new float[]{ newDelta, newVel };
    }

    public static void lookAt(@Nullable Vec3d entityPos, @Nullable BlockPos blockPos,
                              float deltaTime) {
        if (entityPos == null && blockPos == null) return;
        if (entityPos != null && blockPos != null) return;

        PlayerEntity player = GeneralUtils.getPlayer();
        if (player == null) return;

        at.rewrite.ModConfig cfg = ConfigManager.config;

        // Aim at upper chest (~80% height) — not eye level, not centre.
        // Feels natural and still registers hits.
        Vec3d targetBase = entityPos != null ? entityPos : Vec3d.ofCenter(blockPos);

        // New target acquired
        if (lastTargetBase == null || targetBase.squaredDistanceTo(lastTargetBase) > 9.0) {
            lastTargetBase = targetBase;
            yawVelocity   = 0f;
            pitchVelocity = 0f;
            hasOvershot   = false;

            // Randomise aim offset — horizontal spread wider than vertical
            aimOffsetYaw   = (random.nextFloat() - 0.5f) * cfg.aimSlop * 2f;
            aimOffsetPitch = (random.nextFloat() - 0.5f) * cfg.aimSlop;

            // Randomise smoothing ±25% so no two flicks are identical
            float variance = 0.25f;
            thisSmoothing  = cfg.smoothing * (1f + (random.nextFloat() - 0.5f) * variance * 2f);

            // Human reaction delay: 80–200ms, randomised
            reactionTimer  = 0.08f + random.nextFloat() * 0.12f;

            // Small overshoot — humans frequently go slightly past and correct
            overshootAmount = (random.nextFloat() * cfg.overshoot);
        }

        // Reaction delay — don't move yet
        reactionTimer -= deltaTime;
        if (reactionTimer > 0f) return;

        float[] angles    = getAnglesTo(targetBase, player.getEyePos());
        float targetYaw   = angles[0] + aimOffsetYaw;
        float targetPitch = angles[1] + aimOffsetPitch;

        float yawDiff   = angleDiff(player.getYaw(),   targetYaw);
        float pitchDiff = angleDiff(player.getPitch(), targetPitch);
        float totalDist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // Apply overshoot by extending the target just past the aim point
        // — only on the way in, not after we've already overshot
        float overshootBias = 0f;
        if (!hasOvershot && totalDist < 5f && overshootAmount > 0f) {
            overshootBias  = overshootAmount;
            hasOvershot    = true;
        }

        float effectiveYaw   = targetYaw   + (yawDiff   > 0 ? overshootBias : -overshootBias);
        float effectivePitch = targetPitch + (pitchDiff > 0 ? overshootBias * 0.5f : -overshootBias * 0.5f);

        if (totalDist < cfg.threshold) {
            // Settled — kill velocity but don't reset target so it tracks movement
            yawVelocity   = 0f;
            pitchVelocity = 0f;
            return;
        }

        float[] yawR   = springStep(player.getYaw(),   effectiveYaw,   yawVelocity,   thisSmoothing, deltaTime);
        float[] pitchR = springStep(player.getPitch(), effectivePitch, pitchVelocity, thisSmoothing, deltaTime);

        // Clamp max movement per frame — prevents first-frame snap from far away
        float yawDelta   = MathHelper.clamp(yawR[0],   -cfg.maxVelocity * deltaTime, cfg.maxVelocity * deltaTime);
        float pitchDelta = MathHelper.clamp(pitchR[0], -cfg.maxVelocity * deltaTime, cfg.maxVelocity * deltaTime);

        yawVelocity   = yawR[1];
        pitchVelocity = pitchR[1];

        player.setYaw(player.getYaw()     + yawDelta);
        player.setPitch(player.getPitch() + pitchDelta);
    }

    public static void resetVelocity() {
        yawVelocity    = 0f;
        pitchVelocity  = 0f;
        lastTargetBase = null;
        reactionTimer  = 0f;
        hasOvershot    = false;
    }

    public static boolean isLookingAt(String blockId) {
        MinecraftClient client = GeneralUtils.getClient();
        if (client.crosshairTarget == null) return false;
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) return false;
        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        Block block = client.world.getBlockState(hit.getBlockPos()).getBlock();
        return Registries.BLOCK.getId(block).toString().equals(blockId);
    }
}