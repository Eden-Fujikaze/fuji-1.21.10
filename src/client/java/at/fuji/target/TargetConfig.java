package at.fuji.target;

public class TargetConfig {
    public String mobName;
    public boolean waypointEnabled;
    public boolean tracerEnabled;
    public float radius;
    public volatile net.minecraft.world.phys.Vec3 currentPos;

    public TargetConfig(String mobName, boolean waypointEnabled, boolean tracerEnabled, float radius) {
        this.mobName = mobName;
        this.waypointEnabled = waypointEnabled;
        this.tracerEnabled = tracerEnabled;
        this.radius = radius;
    }
}