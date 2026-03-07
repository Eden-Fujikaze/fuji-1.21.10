package at.rewrite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public class ConfigManager {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rewrite.json");

    public static ModConfig config = new ModConfig();

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                config = new Gson().fromJson(r, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        save(); // write defaults if file doesn't exist yet
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(config, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}