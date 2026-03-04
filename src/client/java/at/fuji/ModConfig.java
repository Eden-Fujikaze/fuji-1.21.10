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

    /**
     * When true, the BazaarWorker cancels filled buy orders instead of flipping
     * them, picks up the items from stash, and sells them directly to the NPC
     * via /trades — but only when npcSellPrice > buyOrderPrice.
     */
    public boolean npcSellMode = false;

    /**
     * Debug mode — limits each purchase to exactly 1 item so you can verify
     * the full buy→flip cycle works without committing real coin volume.
     * BazaarWorker should check this and cap purchasableAmount at 1.
     */
    public boolean debugMode = false;

    /**
     * Minimum sells-per-hour required for an item to be considered.
     * Filters out slow-moving items whose orders may sit for hours.
     * Calculated as: min(buyMovingWeek, sellMovingWeek) / askPrice / HOURS_PER_WEEK
     * Default: 50 items/hr (~1,200/day).
     */
    public int minSellsPerHour = 50;

    /**
     * Minimum raw coin volume on the slower side (min of buy/sell moving week)
     * required for an item to be considered. Guards against illiquid items where
     * a single large order can distort the weekly average.
     * Default: 500,000 coins/week.
     */
    public int minWeeklyVolume = 500_000;

    /**
     * Minimum total profit expected from one full order batch
     * (profitPerItem * purchasableAmount). Filters out items where the spread
     * looks fine individually but the batch is too small to be worthwhile.
     * Default: 10,000 coins.
     */
    public int minProfitPerHour = 10_000;

    public static class SavedTarget {
        public String mobName;
        public boolean waypointEnabled;
        public boolean tracerEnabled;
        public float radius;
    }

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null)
            load();
        return instance;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            System.out.println("[Fuji] No config found, using defaults.");
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
            // Migrate: ensure new numeric fields have sensible defaults if absent from old
            // config
            if (instance.minSellsPerHour <= 0)
                instance.minSellsPerHour = 50;
            if (instance.minWeeklyVolume <= 0)
                instance.minWeeklyVolume = 500_000;
            if (instance.minProfitPerHour < 0)
                instance.minProfitPerHour = 10_000;
            System.out.println("[Fuji] Config loaded. targets=" + instance.targets.size()
                    + " blacklist=" + instance.bazaarBlacklist.size()
                    + " npcSellMode=" + instance.npcSellMode
                    + " debugMode=" + instance.debugMode
                    + " minSellsPerHour=" + instance.minSellsPerHour
                    + " minWeeklyVolume=" + instance.minWeeklyVolume
                    + " minProfitPerHour=" + instance.minProfitPerHour);
        } catch (Exception e) {
            System.err.println("[Fuji] Failed to load config: " + e.getMessage());
            instance = new ModConfig();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(instance, w);
            System.out.println("[Fuji] Config saved. targets=" + instance.targets.size()
                    + " blacklist=" + instance.bazaarBlacklist.size()
                    + " npcSellMode=" + instance.npcSellMode
                    + " debugMode=" + instance.debugMode
                    + " minSellsPerHour=" + instance.minSellsPerHour
                    + " minWeeklyVolume=" + instance.minWeeklyVolume
                    + " minProfitPerHour=" + instance.minProfitPerHour);
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
            targets.add(s);
        }
        System.out.println("[Fuji] synced " + targets.size() + " targets.");
    }

    public void loadIntoTargetManager() {
        if (targets == null || targets.isEmpty()) {
            System.out.println("[Fuji] No saved targets, skipping load.");
            return;
        }
        at.fuji.target.TargetManager.targets.clear();
        for (SavedTarget s : targets) {
            at.fuji.target.TargetConfig config = new at.fuji.target.TargetConfig(
                    s.mobName, s.waypointEnabled, s.tracerEnabled, s.radius);
            at.fuji.target.TargetManager.targets.add(config);
        }
        System.out.println("[Fuji] Loaded " + targets.size() + " targets.");
    }
}