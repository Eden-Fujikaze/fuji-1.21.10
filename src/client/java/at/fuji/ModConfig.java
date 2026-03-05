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
    public List<String> bazaarBlacklist = new ArrayList<>(List.of(
            "ESSENCE_UNDEAD", "ESSENCE_DRAGON", "ESSENCE_WITHER",
            "ESSENCE_SPIDER", "ESSENCE_GOLD", "ESSENCE_DIAMOND",
            "ESSENCE_ICE", "ESSENCE_CRIMSON", "ESSENCE_HOLLOW"));

    public boolean npcSellMode = false;
    public boolean debugMode = false;

    // ── Item-selector filters ─────────────────────────────────────────────────

    /** Minimum items/hr on the slower side (fill rate). */
    public int minSellsPerHour = 50;

    /** Minimum raw coin volume (slower side) per week. */
    public int minWeeklyVolume = 500_000;

    /**
     * Minimum absolute spread in coins (ask - bid). Filters tax-eaten
     * micro-spreads.
     */
    public int minMargin = 100;

    /** Minimum number of active sell orders. 0 = disabled. */
    public int minSellOrders = 0;

    /** Minimum profit/hr. Togglable. */
    public int minProfitPerHour = 10_000;
    public boolean minProfitPerHourEnabled = false;

    /** Minimum total profit from one full order. Togglable. */
    public int minTotalProfit = 50_000;
    public boolean minTotalProfitEnabled = false;

    // ── Saved targets ─────────────────────────────────────────────────────────

    public static class SavedTarget {
        public String mobName;
        public boolean waypointEnabled;
        public boolean tracerEnabled;
        public float radius;
        public boolean enabled = true;
        public boolean alertEnabled = false;
        public boolean showDistanceEnabled = false;
        public int color = 0xFFB044FF;
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null)
            load();
        return instance;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            instance = new ModConfig();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            instance = new Gson().fromJson(r, ModConfig.class);
            if (instance == null)
                instance = new ModConfig();
            if (instance.targets == null)
                instance.targets = new ArrayList<>();
            if (instance.bazaarBlacklist == null)
                instance.bazaarBlacklist = new ArrayList<>();
            if (instance.minSellsPerHour <= 0)
                instance.minSellsPerHour = 50;
            if (instance.minWeeklyVolume <= 0)
                instance.minWeeklyVolume = 500_000;
            if (instance.minProfitPerHour < 0)
                instance.minProfitPerHour = 10_000;
            if (instance.minTotalProfit < 0)
                instance.minTotalProfit = 50_000;
            if (instance.minMargin < 0)
                instance.minMargin = 100;
            if (instance.minSellOrders < 0)
                instance.minSellOrders = 0;
            System.out.println("[Fuji] Config loaded.");
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

    public void syncFromTargetManager() {
        targets.clear();
        for (TargetConfig t : at.fuji.target.TargetManager.targets) {
            SavedTarget s = new SavedTarget();
            s.mobName = t.mobName;
            s.waypointEnabled = t.waypointEnabled;
            s.tracerEnabled = t.tracerEnabled;
            s.radius = t.radius;
            s.enabled = t.enabled;
            s.alertEnabled = t.alertEnabled;
            s.showDistanceEnabled = t.showDistanceEnabled;
            s.color = t.color;
            targets.add(s);
        }
    }

    public void loadIntoTargetManager() {
        if (targets == null || targets.isEmpty())
            return;
        at.fuji.target.TargetManager.targets.clear();
        for (SavedTarget s : targets) {
            TargetConfig c = new TargetConfig(
                    s.mobName, s.waypointEnabled, s.tracerEnabled, s.radius);
            c.enabled = s.enabled;
            c.alertEnabled = s.alertEnabled;
            c.showDistanceEnabled = s.showDistanceEnabled;
            c.color = s.color;
            at.fuji.target.TargetManager.targets.add(c);
        }
        System.out.println("[Fuji] Loaded " + targets.size() + " targets.");
    }
}