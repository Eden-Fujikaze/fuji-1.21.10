package at.fuji.target;

import java.util.Comparator;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class EntityUtils {

    public static Vec3 findTarget(String name, float radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return null;

        String normalizedSearchName = name.toLowerCase();

        return mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(radius))
                .stream()
                .filter(e -> {
                    String entityName = e.getName().getString().toLowerCase();
                    return entityName.contains(normalizedSearchName);
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)))
                .map(e -> e.position().add(0, e.getBbHeight() / 2, 0))
                .orElse(null);
    }
}