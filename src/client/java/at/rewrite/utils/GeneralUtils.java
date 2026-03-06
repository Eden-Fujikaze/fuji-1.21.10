package at.rewrite.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.world.World;

public class GeneralUtils {
    public static PlayerEntity getPlayer() {
        return getClient().player;
    }

    public static MinecraftClient getClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null)
            return null;
        return client;
    }

    public static World getWorld() {
        World world = getClient().world;
        if (world == null)
            return null;
        return world;
    }

    public static Scoreboard getScoreboard() {
        World world = getWorld();
        if (world == null)
            return null;
        return world.getScoreboard();
    }

    public static Camera getCamera() {
        Camera camera = getClient().gameRenderer.getCamera();
        if (camera == null)
            return null;
        return camera;
    }
}
