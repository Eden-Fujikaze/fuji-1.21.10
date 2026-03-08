package at.rewrite;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public List<String> targetBlocks = new ArrayList<>(List.of(
            "minecraft:prismarine", "minecraft:prismarine_bricks", "minecraft:dark_prismarine",
            "minecraft:light_blue_wool"));
    public List<String> targetEntities = new ArrayList<>(List.of(
            "Chill", "Shulker"
            ));
    // ── Camera / aim tuning (was FujiConfig) ─────────────────────────────────

    /** How close the crosshair must be to the target before mining starts. */
    public float smoothing   = 8.0f;   // base tracking speed
    public float maxVelocity = 40.0f;  // degrees per second (not per frame now)
    public float threshold   = 1.5f;   // dead zone — slightly generous
    public float aimSlop     = 4.0f;   // off-centre spread
    public float overshoot   = 2.5f;   // degrees past target before correcting
}