package at.rewrite.mob;

import at.rewrite.ConfigManager;
import at.rewrite.utils.PlayerUtils;
import at.rewrite.utils.WorldUtils;
import net.minecraft.util.math.Vec3d;

import javax.swing.text.html.parser.Entity;
import java.util.List;

public class AutoHit {
    public static boolean enabled = false;
    public static String[] entities = ConfigManager.config.targetBlocks.toArray(new String[0]);

    public static void enable() {
        enabled = true;
    }
    public static void disable() {
        enabled = false;
    }
    public static void reload() {
        entities = ConfigManager.config.targetEntities.toArray(new String[0]);
    }

    public static void tick(float deltatime, float radius) {
        Vec3d entityPos = WorldUtils.findEntity("Zombie", radius);
        PlayerUtils.lookAt(entityPos, null, deltatime);
    }
}
