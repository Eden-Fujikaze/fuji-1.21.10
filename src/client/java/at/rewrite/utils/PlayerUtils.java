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

public class PlayerUtils {

    private static final float SPEED = 0.1f;
    public static float THRESHOLD = 0.1f;

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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

    public static void lookAt(@Nullable Vec3d entityPos, @Nullable BlockPos blockPos) {
        if (entityPos == null && blockPos == null)
            return;
        if (entityPos != null && blockPos != null)
            return;

        PlayerEntity player = GeneralUtils.getPlayer();
        if (player == null)
            return;

        Vec3d target = (entityPos != null) ? entityPos : Vec3d.ofCenter(blockPos);
        float[] angles = getAnglesTo(target, player.getEyePos());

        player.setYaw(lerp(player.getYaw(), angles[0], SPEED));
        player.setPitch(lerp(player.getPitch(), angles[1], SPEED));
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