package at.rewrite.mob;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.minecraft.util.math.Vec3d;

public class config {
    public String mobName;
    public boolean waypointEnabled;
    public boolean tracerEnabled;
    public boolean enabled = true;
    public boolean alertEnabled = true;
    public float radius;
    public int color = 0xFFB044FF;

    public volatile Vec3d currentPos;

    public config(String mobName, boolean waypointEnabled, boolean tracerEnabled, float radius) {
        this.mobName = mobName;
        this.waypointEnabled = waypointEnabled;
        this.tracerEnabled = tracerEnabled;
        this.radius = radius;
    }
}
