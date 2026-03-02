package at.fuji.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Client-only utility for reading the sidebar scoreboard.
 */
public class ScoreboardUtils {
    public static final ObjectArrayList<Text> TEXT_SCOREBOARD = new ObjectArrayList<>();
    public static final ObjectArrayList<String> STRING_SCOREBOARD = new ObjectArrayList<>();

    // Aggressive regex: Matches '§' and literally ANY character immediately
    // following it.
    private static final Pattern AGGRESSIVE_FORMATTING_PATTERN = Pattern.compile("§.");

    // --- HELPER METHODS ---

    /**
     * Searches for a line containing the specific text and returns its index in the
     * cleaned STRING_SCOREBOARD list.
     * Returns -1 if not found.
     */
    public static int getLineIndexByContent(String search) {
        update(); // Call update before processing
        for (int i = 0; i < STRING_SCOREBOARD.size(); i++) {
            if (STRING_SCOREBOARD.get(i).contains(search)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the raw purse value (e.g., 16223171) from the sidebar.
     */
    public static long getPurseValue() {
        update(); // Call update before processing
        for (String line : STRING_SCOREBOARD) {
            if (line.contains("Purse:")) {
                // Remove "Purse:", commas, and spaces, then parse the number
                String cleaned = line.replace("Purse:", "").replace(",", "").trim();
                try {
                    return Long.parseLong(cleaned);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Returns the location string (line containing the ⏣ symbol).
     */
    public static String getLocation() {
        update(); // Call update before processing
        for (String line : STRING_SCOREBOARD) {
            if (line.contains("⏣")) {
                return line.trim();
            }
        }
        return "Unknown";
    }

    // --- INTERNAL UPDATE LOGIC ---

    /**
     * Updates the sidebar scoreboard text arrays.
     * Called internally by helper methods.
     */
    private static void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null)
            return;

        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null)
            return;

        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null)
            return;

        ObjectArrayList<Text> textLines = new ObjectArrayList<>();
        ObjectArrayList<String> stringLines = new ObjectArrayList<>();

        List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(sidebar).stream()
                .sorted(Comparator.comparing(ScoreboardEntry::value))
                .toList();

        for (ScoreboardEntry entry : entries) {
            String owner = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(owner);
            Text displayText = entry.display() != null ? entry.display() : Text.literal(owner);

            if (team != null) {
                displayText = Team.decorateName(team, displayText);
            }

            textLines.add(displayText);
            stringLines.add(cleanString(displayText.getString()));
        }

        Text title = sidebar.getDisplayName();
        textLines.add(title);
        stringLines.add(cleanString(title.getString()));

        Collections.reverse(textLines);
        Collections.reverse(stringLines);

        TEXT_SCOREBOARD.clear();
        STRING_SCOREBOARD.clear();
        TEXT_SCOREBOARD.addAll(textLines);
        STRING_SCOREBOARD.addAll(stringLines);
    }

    private static String cleanString(String input) {
        return AGGRESSIVE_FORMATTING_PATTERN.matcher(input).replaceAll("");
    }
}