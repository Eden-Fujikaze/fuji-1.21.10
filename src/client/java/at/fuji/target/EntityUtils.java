package at.fuji.target;

import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class EntityUtils {

    public static Vec3d findTarget(String name, float radius) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null)
            return null;

        String normalizedSearchName = name.toLowerCase();

        return mc.world.getNonSpectatingEntities(LivingEntity.class,
                mc.player.getBoundingBox().expand(radius))
                .stream()
                .filter(e -> {
                    String entityName = e.getName().getString().toLowerCase();
                    return entityName.contains(normalizedSearchName);
                })
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)))
                .map(e -> e.getEntityPos().add(0, e.getHeight() / 2, 0))
                .orElse(null);
    }
}