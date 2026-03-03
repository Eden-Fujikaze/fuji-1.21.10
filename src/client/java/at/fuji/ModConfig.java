package at.fuji;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import at.fuji.target.TargetConfig;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ModConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("fuji.json");

    public String apiKey = "";
    public List<SavedTarget> targets = new ArrayList<>();

    public static class SavedTarget {
        public String mobName;
        public boolean waypointEnabled;
        public boolean tracerEnabled;
        public float radius;
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null)
            load();
        return instance;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            instance = new ModConfig();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            instance = new Gson().fromJson(r, ModConfig.class);
            if (instance == null)
                instance = new ModConfig();
        } catch (Exception e) {
            System.err.println("[Fuji] Failed to load config: " + e.getMessage());
            instance = new ModConfig();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(instance, w);
        } catch (Exception e) {
            System.err.println("[Fuji] Failed to save config: " + e.getMessage());
        }
    }

    // ── Target helpers ────────────────────────────────────────────────────────

    public void syncFromTargetManager() {
        targets.clear();
        for (TargetConfig t : at.fuji.target.TargetManager.targets) {
            SavedTarget s = new SavedTarget();
            s.mobName = t.mobName;
            s.waypointEnabled = t.waypointEnabled;
            s.tracerEnabled = t.tracerEnabled;
            s.radius = t.radius;
            targets.add(s);
        }
    }

    public void loadIntoTargetManager() {
        at.fuji.target.TargetManager.targets.clear();
        for (SavedTarget s : targets) {
            at.fuji.target.TargetManager.addTarget(
                    s.mobName, s.waypointEnabled, s.tracerEnabled, s.radius);
        }
    }
}