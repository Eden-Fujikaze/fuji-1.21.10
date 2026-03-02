package at.fuji.target;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
    }

    public static void removeTarget(String mobNameFilter) {
        if (mobNameFilter == null || mobNameFilter.isEmpty())
            return;

        Pattern pattern = createPattern(mobNameFilter);

        targets.removeIf(config -> pattern.matcher(config.mobName).matches());
    }

    public static void refresh() {
        for (TargetConfig config : targets) {
            if (config.mobName != null) {
                Vec3 pos = EntityUtils.findTarget(config.mobName, config.radius);
                config.currentPos = pos;
            }
        }
    }
}