package at.rewrite.utils;

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

    public static float THRESHOLD = 0.1f;

    private static float yawVelocity = 0f;
    private static float pitchVelocity = 0f;

    private static float noiseYaw = 0f;
    private static float noisePitch = 0f;
    private static final Random random = new Random();

    private static final float ACCELERATION = 0.8f;
    private static final float FRICTION = 0.1f;
    private static final float MAX_VELOCITY = 2.0f;
    private static final float NOISE_STRENGTH = 1.6f;
    private static final float NOISE_FADE_DIST = 6.0f;
    private static final float NOISE_DRIFT = 0.15f;

    private static float wrapAngle(float angle) {
        return ((angle % 360f) + 360f) % 360f;
    }

    private static float angleDiff(float current, float target) {
        float diff = wrapAngle(target) - wrapAngle(current);
        return ((diff + 540f) % 360f) - 180f;
    }

    private static float[] getAnglesTo(Vec3d target, Vec3d eyes) {
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(MathHelper.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(MathHelper.atan2(dy, horizontalDist));

        return new float[] { yaw, pitch };
    }

    public static void lookAt(@Nullable Vec3d entityPos, @Nullable BlockPos blockPos, float deltaTime) {
        if (entityPos == null && blockPos == null)
            return;
        if (entityPos != null && blockPos != null)
            return;

        PlayerEntity player = GeneralUtils.getPlayer();
        if (player == null)
            return;

        Vec3d target = (entityPos != null) ? entityPos : Vec3d.ofCenter(blockPos);
        float[] angles = getAnglesTo(target, player.getEyePos());

        float targetYaw = angles[0];
        float targetPitch = angles[1];

        float yawDiff = angleDiff(player.getYaw(), targetYaw);
        float pitchDiff = angleDiff(player.getPitch(), targetPitch);
        float totalDist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // Scale factor — deltaTime of 1.0 == one full tick (1/20s)
        float scale = deltaTime * 3f;

        // Frame-rate-independent exponential decay for noise
        float noiseDecay = (float) Math.pow(0.92f, scale * 20f);

        float noiseFactor = Math.min(1f, totalDist / NOISE_FADE_DIST);
        noiseYaw += (random.nextFloat() - 0.5f) * NOISE_DRIFT * scale;
        noisePitch += (random.nextFloat() - 0.5f) * NOISE_DRIFT * scale;
        noiseYaw *= noiseDecay;
        noisePitch *= noiseDecay;
        noiseYaw = MathHelper.clamp(noiseYaw, -NOISE_STRENGTH, NOISE_STRENGTH);
        noisePitch = MathHelper.clamp(noisePitch, -NOISE_STRENGTH, NOISE_STRENGTH);

        yawVelocity += (yawDiff + noiseYaw * noiseFactor) * ACCELERATION * scale;
        pitchVelocity += (pitchDiff + noisePitch * noiseFactor) * ACCELERATION * scale;

        yawVelocity = MathHelper.clamp(yawVelocity, -MAX_VELOCITY, MAX_VELOCITY);
        pitchVelocity = MathHelper.clamp(pitchVelocity, -MAX_VELOCITY, MAX_VELOCITY);

        player.setYaw(player.getYaw() + yawVelocity * scale);
        player.setPitch(player.getPitch() + pitchVelocity * scale);

        // Frame-rate-independent friction decay
        float frictionDecay = (float) Math.pow(FRICTION, scale * 20f);
        float nearFactor = 1f - Math.min(1f, totalDist / 3f) * 0.15f;
        yawVelocity *= frictionDecay * nearFactor;
        pitchVelocity *= frictionDecay * nearFactor;
    }

    public static void resetVelocity() {
        yawVelocity = 0f;
        pitchVelocity = 0f;
        noiseYaw = 0f;
        noisePitch = 0f;
    }

    public static boolean isLookingAt(String blockId) {
        MinecraftClient client = GeneralUtils.getClient();
        if (client.crosshairTarget == null)
            return false;
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK)
            return false;

        BlockHitResult hitResult = (BlockHitResult) client.crosshairTarget;
        Block block = client.world.getBlockState(hitResult.getBlockPos()).getBlock();
        String id = Registries.BLOCK.getId(block).toString();

        return id.equals(blockId);
    }
}