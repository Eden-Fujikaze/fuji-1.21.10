package at.fuji.target;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import at.fuji.ModConfig;
import net.minecraft.util.math.Vec3d;

public class TargetManager {

    public static final List<TargetConfig> targets = new ArrayList<>();

    private static Pattern createPattern(String filter) {
        return Pattern.compile("(?i).*" + Pattern.quote(filter) + ".*");
    }

    public static void addTarget(String mobName, boolean waypoint, boolean tracer, float radius) {
        if (mobName == null || mobName.isEmpty())
            return;
        TargetConfig config = new TargetConfig(mobName, waypoint, tracer, radius);
        targets.add(config);
        saveConfig();
    }

    public static void toggleWaypoint(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;
        Pattern pattern = createPattern(mobNameFilter);
        for (TargetConfig config : targets) {
            if (pattern.matcher(config.mobName).matches()) {
                config.waypointEnabled = !config.waypointEnabled;
            }
        }
        saveConfig();
    }

    public static void toggleTracer(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;
        Pattern pattern = createPattern(mobNameFilter);
        for (TargetConfig config : targets) {
            if (pattern.matcher(config.mobName).matches()) {
                config.tracerEnabled = !config.tracerEnabled;
            }
        }
        saveConfig();
    }

    public static void removeTarget(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;
        Pattern pattern = createPattern(mobNameFilter);
        targets.removeIf(config -> pattern.matcher(config.mobName).matches());
        saveConfig();
    }

    public static void refresh() {
        for (TargetConfig config : targets) {
            if (config.mobName != null) {
                Vec3d pos = EntityUtils.findTarget(config.mobName, config.radius);
                config.currentPos = pos;
                System.out.println("[TargetManager] " + config.mobName + " radius=" + config.radius + " pos=" + pos
                        + " waypoint=" + config.waypointEnabled + " tracer=" + config.tracerEnabled);
            }
        }
    }

    private static void saveConfig() {
        ModConfig.get().syncFromTargetManager();
        ModConfig.save();
    }
}