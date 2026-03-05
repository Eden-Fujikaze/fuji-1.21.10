package at.fuji.target;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import at.fuji.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class TargetManager {

    public static final List<TargetConfig> targets = new ArrayList<>();

    private static Pattern createPattern(String filter) {
        return Pattern.compile("(?i).*" + Pattern.quote(filter) + ".*");
    }

    public static void addTarget(String mobName, boolean waypoint, boolean tracer, float radius) {
        if (mobName == null || mobName.isEmpty())
            return;
        targets.add(new TargetConfig(mobName, waypoint, tracer, radius));
        saveConfig();
    }

    public static void toggleWaypoint(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;
        Pattern p = createPattern(mobNameFilter);
        for (TargetConfig cfg : targets)
            if (p.matcher(cfg.mobName).matches())
                cfg.waypointEnabled = !cfg.waypointEnabled;
        saveConfig();
    }

    public static void toggleTracer(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;
        Pattern p = createPattern(mobNameFilter);
        for (TargetConfig cfg : targets)
            if (p.matcher(cfg.mobName).matches())
                cfg.tracerEnabled = !cfg.tracerEnabled;
        saveConfig();
    }

    public static void removeTarget(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;
        Pattern p = createPattern(mobNameFilter);
        targets.removeIf(cfg -> p.matcher(cfg.mobName).matches());
        saveConfig();
    }

    public static void refresh() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (TargetConfig config : targets) {
            if (config.mobName == null)
                continue;
            if (!config.enabled) {
                config.currentPos = null;
                continue;
            }
            Vec3d prev = config.currentPos;
            Vec3d pos = EntityUtils.findTarget(config.mobName, config.radius);
            config.currentPos = pos;

            // Sound alert: fires once when target newly appears
            if (config.alertEnabled && prev == null && pos != null && mc.player != null) {
                mc.execute(() -> mc.player.playSound(
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f));
            }
        }
    }

    public static void saveConfig() {
        ModConfig.get().syncFromTargetManager();
        ModConfig.save();
    }
}