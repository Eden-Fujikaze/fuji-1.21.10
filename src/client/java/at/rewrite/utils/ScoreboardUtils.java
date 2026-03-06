package at.rewrite.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import at.fuji.utils.StripUtils;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

public class ScoreboardUtils {
    private static Map<String, String> rawEntries = new HashMap<>();

    private static void update() {
        rawEntries.clear();
        Scoreboard scoreboard = GeneralUtils.getScoreboard();
        if (scoreboard == null) {
            return;
        }
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(sidebar);
        for (ScoreboardEntry input : entries) {
            String[] a = remapEntry(input, scoreboard);
            if (a == null)
                continue;
            rawEntries.put(a[0], a[1]);
        }
    }

    public static String findValue(String keyword, boolean exact) {
        update();
        for (Map.Entry<String, String> entry : rawEntries.entrySet()) {
            boolean result = TextUtils.compareStrings(entry.getKey(), keyword, exact);
            if (result) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String[] remapEntry(ScoreboardEntry base, Scoreboard scoreboard) {
        String owner = base.owner();
        Team team = scoreboard.getScoreHolderTeam(owner);
        Text displayText = base.display() != null ? base.display() : Text.literal(owner);

        if (team != null) {
            String splittable = StripUtils.stripColorCodes(Team.decorateName(team, displayText).getString());
            if (splittable.contains(":")) {
                String[] split = splittable.split(":");
                return split;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}