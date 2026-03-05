package at.fuji.target;

public class TargetConfig {
    public String mobName;
    public boolean waypointEnabled;
    public boolean tracerEnabled;
    public boolean enabled = true;
    public boolean alertEnabled = false;
    public boolean showDistanceEnabled = false;
    public float radius;
    public int color = 0xFFB044FF; // default purple

    public volatile net.minecraft.util.math.Vec3d currentPos;

    public TargetConfig(String mobName, boolean waypointEnabled, boolean tracerEnabled, float radius) {
        this.mobName = mobName;
        this.waypointEnabled = waypointEnabled;
        this.tracerEnabled = tracerEnabled;
        this.radius = radius;
    }
}