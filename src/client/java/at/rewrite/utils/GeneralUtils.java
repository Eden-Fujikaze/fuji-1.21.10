package at.rewrite.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.world.World;

public class GeneralUtils {
    public static PlayerEntity getPlayer() {
        assert getClient() != null;
        return getClient().player;
    }

    public static MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }

    public static World getWorld() {
        assert getClient() != null;
        return getClient().world;
    }

    public static Scoreboard getScoreboard() {
        World world = getWorld();
        if (world == null)
            return null;
        return world.getScoreboard();
    }

    public static Camera getCamera() {
        assert getClient() != null;
        return getClient().gameRenderer.getCamera();
    }
}
