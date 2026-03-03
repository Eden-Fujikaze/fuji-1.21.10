package at.fuji.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

public class LoreUtils {
    public static boolean startsWith(ItemStack stack, String prefix) {
        return getSlotName(stack).startsWith(prefix);
    }

    public static String getSlotName(ItemStack stack) {
        return StripUtils.stripColorCodes(stack.getName().getString());
    }

    public static boolean loreContains(ItemStack stack, String text) {
        for (String line : getLore(stack)) {
            if (line.contains(text))
                return true;
        }
        return false;
    }

    public static List<String> getLore(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        net.minecraft.item.tooltip.TooltipType tooltipType = net.minecraft.item.tooltip.TooltipType.BASIC;
        for (net.minecraft.text.Text line : stack.getTooltip(
                net.minecraft.item.Item.TooltipContext.DEFAULT, null, tooltipType)) {
            lines.add(StripUtils.stripColorCodes(line.getString()));
        }
        return lines;
    }

    public static int parseAmount(ItemStack stack, String linePrefix) {
        for (String line : getLore(stack)) {
            if (line.startsWith(linePrefix)) {
                try {
                    return Integer.parseInt(
                            line.replace(linePrefix, "")
                                    .replace("x", "")
                                    .replace(",", "")
                                    .trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }
}
