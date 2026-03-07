package at.rewrite.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class WorldUtils {

    private static boolean hasLineOfSight(World world, PlayerEntity player, BlockPos target) {
        Vec3d eyes = player.getEyePos();
        Vec3d targetVec = Vec3d.ofCenter(target);

        RaycastContext ctx = new RaycastContext(
                eyes, targetVec,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player);

        BlockHitResult result = world.raycast(ctx);
        return result.getBlockPos().equals(target);
    }
    public static Vec3d findEntity(String name, float radius) {
        MinecraftClient mc = GeneralUtils.getClient();
        if (mc.world == null || mc.player ==null) {
            return null;
        }

        String normalizedName = name.toLowerCase();

        return mc.world.getNonSpectatingEntities(LivingEntity.class,
                mc.player.getBoundingBox().expand(radius))
                .stream()
                .filter(e-> {
                    String entityName = e.getName().getString().toLowerCase();
                    return entityName.contains(normalizedName);
                })
                .min(Comparator.comparingDouble(e-> e.squaredDistanceTo(mc.player)))
                .map(e -> e.getEntityPos().add(0, e.getHeight() / 2, 0))
                .orElse(null);
    }
    public static BlockPos findBlock(int radius, String[] validBlocks) {
        PlayerEntity player = GeneralUtils.getPlayer();
        if (player == null)
            return null;

        BlockPos playerPos = player.getBlockPos();
        World world = GeneralUtils.getWorld();
        List<BlockPos> results = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(
                playerPos.add(-radius, -radius, -radius),
                playerPos.add(radius, radius, radius))) {

            BlockState state = world.getBlockState(pos);
            String id = Registries.BLOCK.getId(state.getBlock()).toString();

            if (Arrays.stream(validBlocks).anyMatch(id::equals) && hasLineOfSight(world, player, pos)) {
                results.add(pos.toImmutable());
            }
        }

        return results.stream()
                .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(playerPos)))
                .orElse(null);
    }
}