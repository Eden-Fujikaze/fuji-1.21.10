package at.fuji.ui;

import at.fuji.ModConfig;
import at.fuji.bazaar.BazaarWorker;
import at.fuji.bazaar.ItemScore;
import at.fuji.bazaar.ItemSelector;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * NOTE: Add the following to ItemSelector so the preview panel can read it:
 *
 * public static volatile ItemScore lastBest = null;
 *
 * Then after `ItemScore best = candidates.get(0);` add:
 *
 * ItemSelector.lastBest = best;
 */
public class BazaarPanel implements Sidebar {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COL_PANEL = 0xFF12121A;
    private static final int COL_ROW_EVEN = 0xFF15151F;
    private static final int COL_ROW_HOVER = 0xFF1E1E2E;
    private static final int COL_BORDER = 0xFF2A2A3F;
    private static final int COL_ACCENT = 0xFFFF6644;
    private static final int COL_RED = 0xFFFF4455;
    private static final int COL_GREEN = 0xFF44FF88;
    private static final int COL_YELLOW = 0xFFFFDD44;
    private static final int COL_BLUE = 0xFF44AAFF;
    private static final int COL_TEXT = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_SCROLLBAR = 0xFF3A3A5A;
    private static final int COL_SCROLL_TH = 0xFF6060A0;
    private static final int COL_SETTINGS = 0xFF0D0D16;
    private static final int COL_PREVIEW_BG = 0xFF0F0F1A;
    private static final int COL_PREVIEW_HD = 0xFF0A0A0F;
    private static final int COL_DIVIDER = 0xFF222233;

    // ── Main panel layout ─────────────────────────────────────────────────────
    private static final int ROW_HEIGHT = 28;
    private static final int HEADER_H = 32;
    /**
     * Settings block: 3 rows × (14px button + 8px gap) + 6px top pad + 6px bottom
     * pad
     * Row1 y = HEADER_H + 6 (toggles)
     * Row2 y = HEADER_H + 28 (sells/hr, vol/wk)
     * Row3 y = HEADER_H + 50 (min profit)
     * Block h = 50 + 14 + 6 = 70
     */
    private static final int SETTINGS_H = 70;
    private static final int FOOTER_H = 40;
    private static final int PADDING = 14;
    private static final int SB_W = 4;

    // ── Preview panel ─────────────────────────────────────────────────────────
    private static final int PREV_W = 210;
    private static final int PREV_GAP = 6; // gap between main panel right edge and preview
    private static final int PREV_HEADER = 28;
    private static final int PREV_PAD = 10;
    private static final int PREV_ROW = 18; // line height inside preview

    // ── Settings toggle buttons ───────────────────────────────────────────────
    private static final int TOG_W = 72;
    private static final int TOG_H = 14;

    // Row 1 x-offsets (relative to px)
    private static final int R1_NPC_X = PADDING;
    private static final int R1_DBG_X = PADDING + TOG_W + 6;
    private static final int R1_PREV_X = PADDING + (TOG_W + 6) * 2; // PREVIEW toggle

    // Row 2: Sells/hr [-] [value] [+]
    private static final int R2_SLB_X = PADDING; // label
    private static final int R2_SMN_X = PADDING + 52; // [-]
    private static final int R2_SV_X = PADDING + 68; // value
    private static final int R2_SPL_X = PADDING + 100; // [+]
    // Row 2: Vol/wk [-] [value] [+]
    private static final int R2_VLB_X = PADDING + 120;
    private static final int R2_VMN_X = PADDING + 162;
    private static final int R2_VV_X = PADDING + 178;
    private static final int R2_VPL_X = PADDING + 218;

    // Row 3: Min Profit [-] [value] [+]
    private static final int R3_PLB_X = PADDING; // label
    private static final int R3_PMN_X = PADDING + 62; // [-]
    private static final int R3_PV_X = PADDING + 78; // value
    private static final int R3_PPL_X = PADDING + 120; // [+]

    // Step sizes
    private static final int STEP_SELLS = 10;
    private static final int STEP_VOL = 100_000;
    private static final int STEP_PROFIT = 5_000;

    // ── State ─────────────────────────────────────────────────────────────────
    private FujiScreen parent;
    private int px, py, pw, ph;

    private int scrollOffset = 0;
    private boolean addRowVisible = false;
    private boolean previewOpen = false;
    private volatile boolean previewLoading = false;
    private TextFieldWidget nameBox;
    private final List<Object> managedWidgets = new ArrayList<>();

    // ── Sidebar contract ──────────────────────────────────────────────────────

    @Override
    public String getLabel() {
        return "BAZAAR";
    }

    @Override
    public int getAccentColor() {
        return COL_ACCENT;
    }

    @Override
    public void init(FujiScreen parent, int panelX, int panelY, int panelW, int panelH) {
        this.parent = parent;
        this.px = panelX;
        this.py = panelY;
        this.pw = panelW;
        this.ph = panelH;
        scrollOffset = 0;
        rebuildWidgets();
    }

    @Override
    public void clearWidgets() {
        managedWidgets.forEach(w -> {
            if (w instanceof net.minecraft.client.gui.widget.ClickableWidget cw)
                parent.removeWidget(cw);
        });
        managedWidgets.clear();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int listTop = py + HEADER_H + SETTINGS_H;
        int listBottom = py + ph - FOOTER_H;
        if (mouseX < px || mouseX > px + pw || mouseY < listTop || mouseY > listBottom)
            return false;
        scrollOffset -= (int) (amount * ROW_HEIGHT);
        clampScroll();
        rebuildWidgets();
        return true;
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────

    private int listViewportH() {
        return ph - HEADER_H - SETTINGS_H - FOOTER_H;
    }

    private int maxScroll() {
        return Math.max(0, ModConfig.get().bazaarBlacklist.size() * ROW_HEIGHT - listViewportH());
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private void addBtn(ButtonWidget btn) {
        managedWidgets.add(btn);
        parent.addWidget(btn);
    }

    private void addBox(TextFieldWidget b) {
        managedWidgets.add(b);
        parent.addWidget(b);
    }

    private void rebuildWidgets() {
        clearWidgets();
        clampScroll();

        ModConfig cfg = ModConfig.get();
        List<String> blist = cfg.bazaarBlacklist;
        int listTop = py + HEADER_H + SETTINGS_H;
        int listBottom = py + ph - FOOTER_H;
        int settTop = py + HEADER_H;

        // ── Row 1: mode toggles ───────────────────────────────────────────────
        int r1Y = settTop + 6;

        addBtn(toggle("NPC SELL", cfg.npcSellMode, px + R1_NPC_X, r1Y, btn -> {
            cfg.npcSellMode = !cfg.npcSellMode;
            ModConfig.save();
            rebuildWidgets();
        }));

        addBtn(toggle("DEBUG", cfg.debugMode, px + R1_DBG_X, r1Y, btn -> {
            boolean turningOn = !cfg.debugMode;
            cfg.debugMode = turningOn;
            ModConfig.save();
            // Restart the bot immediately so the new cap takes effect
            if (turningOn && BazaarWorker.isEnabled()) {
                BazaarWorker.stop();
                new BazaarWorker().start();
            }
            rebuildWidgets();
        }));

        addBtn(toggle("PREVIEW", previewOpen, px + R1_PREV_X, r1Y, btn -> {
            previewOpen = !previewOpen;
            rebuildWidgets();
        }));

        // ── Row 2: Sells/hr and Vol/wk ───────────────────────────────────────
        int r2Y = settTop + 28;

        addBtn(smallBtn("-", px + R2_SMN_X, r2Y, () -> {
            cfg.minSellsPerHour = Math.max(0, cfg.minSellsPerHour - STEP_SELLS);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(smallBtn("+", px + R2_SPL_X, r2Y, () -> {
            cfg.minSellsPerHour += STEP_SELLS;
            ModConfig.save();
            rebuildWidgets();
        }));

        addBtn(smallBtn("-", px + R2_VMN_X, r2Y, () -> {
            cfg.minWeeklyVolume = Math.max(0, cfg.minWeeklyVolume - STEP_VOL);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(smallBtn("+", px + R2_VPL_X, r2Y, () -> {
            cfg.minWeeklyVolume += STEP_VOL;
            ModConfig.save();
            rebuildWidgets();
        }));

        // ── Row 3: Min total profit ───────────────────────────────────────────
        int r3Y = settTop + 50;

        addBtn(smallBtn("-", px + R3_PMN_X, r3Y, () -> {
            cfg.minProfitPerHour = Math.max(0, cfg.minProfitPerHour - STEP_PROFIT);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(smallBtn("+", px + R3_PPL_X, r3Y, () -> {
            cfg.minProfitPerHour += STEP_PROFIT;
            ModConfig.save();
            rebuildWidgets();
        }));

        // ── Preview close + refresh buttons (only when open) ─────────────────
        if (previewOpen) {
            int pvX = px + pw + PREV_GAP;

            // [×] close
            addBtn(smallBtn("\u00D7", pvX + PREV_W - PREV_PAD - 14, py + 7, btn -> {
                previewOpen = false;
                rebuildWidgets();
            }));

            // [↻] refresh — re-runs scoring against current purse, no bot needed
            addBtn(styledButton(previewLoading ? "..." : "\u21BB REFRESH",
                    pvX + PREV_PAD, py + 7, 60, 14, btn -> {
                        if (previewLoading)
                            return;
                        previewLoading = true;
                        rebuildWidgets(); // immediately show "..."
                        double purse = BazaarWorker.lastKnownPurse;
                        ItemSelector.selectBestItem(purse).thenAccept(result -> {
                            ItemSelector.lastBest = result;
                            previewLoading = false;
                            MinecraftClient.getInstance().execute(this::rebuildWidgets);
                        });
                    }));
        }

        // ── Blacklist remove buttons ──────────────────────────────────────────
        for (int i = 0; i < blist.size(); i++) {
            final int idx = i;
            int rowY = listTop + i * ROW_HEIGHT - scrollOffset;
            if (rowY + ROW_HEIGHT <= listTop || rowY >= listBottom)
                continue;
            int btnY = rowY + (ROW_HEIGHT - 14) / 2;
            addBtn(styledButton("REMOVE", px + pw - PADDING - SB_W - 44, btnY, 44, 14, btn -> {
                ModConfig.get().bazaarBlacklist.remove(idx);
                ModConfig.save();
                rebuildWidgets();
            }));
        }

        // ── Footer ─────────────────────────────────────────────────────────────
        int footerY = py + ph - FOOTER_H;
        if (addRowVisible) {
            nameBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                    px + PADDING, footerY + 12, 200, 16, Text.empty());
            nameBox.setPlaceholder(Text.literal("Product ID e.g. ENCHANTED_SUGAR"));
            nameBox.setMaxLength(64);
            addBox(nameBox);
            addBtn(styledButton("ADD", px + PADDING + 208, footerY + 13, 36, 14, btn -> {
                String id = nameBox.getText().trim().toUpperCase().replace(" ", "_");
                if (!id.isEmpty() && !cfg.bazaarBlacklist.contains(id)) {
                    cfg.bazaarBlacklist.add(id);
                    ModConfig.save();
                }
                addRowVisible = false;
                rebuildWidgets();
            }));
            addBtn(styledButton("CANCEL", px + PADDING + 250, footerY + 13, 48, 14, btn -> {
                addRowVisible = false;
                rebuildWidgets();
            }));
        } else {
            addBtn(styledButton("+ BLACKLIST ITEM", px + PADDING, footerY + 13, 110, 14, btn -> {
                addRowVisible = true;
                rebuildWidgets();
            }));
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        ModConfig cfg = ModConfig.get();
        List<String> bl = cfg.bazaarBlacklist;
        int listTop = py + HEADER_H + SETTINGS_H;
        int listBottom = py + ph - FOOTER_H;
        int vpH = listViewportH();
        int contentH = bl.size() * ROW_HEIGHT;
        var tr = MinecraftClient.getInstance().textRenderer;

        // ── Header ────────────────────────────────────────────────────────────
        gfx.fill(px, py, px + pw, py + HEADER_H, 0xFF0A0A0F);
        gfx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_BORDER);
        gfx.drawText(tr, "BAZAAR BLACKLIST",
                px + PADDING, py + (HEADER_H - 8) / 2, COL_ACCENT, false);
        gfx.drawText(tr, bl.size() + " ITEMS",
                px + PADDING + 110, py + (HEADER_H - 8) / 2, COL_TEXT_DIM, false);

        // ── Settings block ────────────────────────────────────────────────────
        int settTop = py + HEADER_H;
        gfx.fill(px, settTop, px + pw, settTop + SETTINGS_H, COL_SETTINGS);
        gfx.fill(px, settTop + SETTINGS_H - 1, px + pw, settTop + SETTINGS_H, COL_BORDER);

        // Row 1 inline text
        int r1Y = settTop + 6;
        if (cfg.debugMode)
            gfx.drawText(tr, "\u26A0 1 item only",
                    px + R1_PREV_X + TOG_W + 8, r1Y + 3, COL_YELLOW, false);

        // Row 2 labels + values
        int r2Y = settTop + 28;
        gfx.drawText(tr, "Sells/hr", px + R2_SLB_X, r2Y + 3, COL_TEXT_DIM, false);
        gfx.drawText(tr, cfg.minSellsPerHour + "/hr",
                px + R2_SV_X + 2, r2Y + 3, COL_TEXT, false);
        gfx.drawText(tr, "Vol/wk", px + R2_VLB_X, r2Y + 3, COL_TEXT_DIM, false);
        gfx.drawText(tr, fmtCoins(cfg.minWeeklyVolume),
                px + R2_VV_X + 2, r2Y + 3, COL_TEXT, false);

        // Row 3 labels + values
        int r3Y = settTop + 50;
        gfx.drawText(tr, "Min profit", px + R3_PLB_X, r3Y + 3, COL_TEXT_DIM, false);
        gfx.drawText(tr, fmtCoins(cfg.minProfitPerHour),
                px + R3_PV_X + 2, r3Y + 3, COL_TEXT, false);

        // ── Scrollable blacklist (scissored) ──────────────────────────────────
        gfx.enableScissor(px, listTop, px + pw - SB_W, listBottom);
        gfx.drawText(tr, "PRODUCT ID",
                px + PADDING, listTop + 2 - scrollOffset, COL_TEXT_DIM, false);
        for (int i = 0; i < bl.size(); i++) {
            int rowY = listTop + i * ROW_HEIGHT - scrollOffset;
            if (rowY + ROW_HEIGHT <= listTop || rowY >= listBottom)
                continue;
            boolean hovered = mouseX >= px && mouseX < px + pw - SB_W
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            gfx.fill(px, rowY, px + pw - SB_W, rowY + ROW_HEIGHT,
                    hovered ? COL_ROW_HOVER : (i % 2 == 0 ? COL_ROW_EVEN : COL_PANEL));
            gfx.fill(px, rowY, px + 2, rowY + ROW_HEIGHT, COL_RED);
            gfx.drawText(tr, bl.get(i),
                    px + PADDING, rowY + (ROW_HEIGHT - 8) / 2, COL_TEXT, false);
            gfx.fill(px, rowY + ROW_HEIGHT - 1, px + pw - SB_W, rowY + ROW_HEIGHT, COL_BORDER);
        }
        gfx.disableScissor();

        // ── Scrollbar ─────────────────────────────────────────────────────────
        int sbX = px + pw - SB_W;
        gfx.fill(sbX, listTop, sbX + SB_W, listBottom, COL_SCROLLBAR);
        if (contentH > vpH) {
            int thumbH = Math.max(16, vpH * vpH / contentH);
            int thumbRange = vpH - thumbH;
            int thumbY = listTop + (maxScroll() > 0 ? thumbRange * scrollOffset / maxScroll() : 0);
            gfx.fill(sbX, thumbY, sbX + SB_W, thumbY + thumbH, COL_SCROLL_TH);
        }

        // ── Footer separator ──────────────────────────────────────────────────
        gfx.fill(px, py + ph - FOOTER_H, px + pw, py + ph - FOOTER_H + 1, COL_BORDER);

        // ── Preview panel (drawn last so it layers on top) ────────────────────
        if (previewOpen) {
            renderPreview(gfx, tr, mouseX, mouseY);
        }
    }

    private void renderPreview(DrawContext gfx, net.minecraft.client.font.TextRenderer tr,
            int mouseX, int mouseY) {
        int pvX = px + pw + PREV_GAP;
        int pvY = py;
        int pvW = PREV_W;
        int pvH = ph;

        // Background + border
        gfx.fill(pvX, pvY, pvX + pvW, pvY + pvH, COL_PREVIEW_BG);
        gfx.fill(pvX, pvY, pvX + 1, pvY + pvH, COL_BORDER); // left edge
        gfx.fill(pvX + pvW - 1, pvY, pvX + pvW, pvY + pvH, COL_BORDER); // right edge
        gfx.fill(pvX, pvY, pvX + pvW, pvY + 1, COL_BORDER); // top edge
        gfx.fill(pvX, pvY + pvH - 1, pvX + pvW, pvY + pvH, COL_BORDER); // bottom edge
        gfx.fill(pvX, pvY + PREV_HEADER - 1, pvX + pvW, pvY + PREV_HEADER, COL_BORDER);

        // Header
        gfx.fill(pvX, pvY, pvX + pvW, pvY + PREV_HEADER, COL_PREVIEW_HD);
        gfx.drawText(tr, "LAST SCORED ITEM",
                pvX + PREV_PAD, pvY + (PREV_HEADER - 8) / 2, COL_ACCENT, false);
        // [×] button is a managed widget — skip drawing it here

        if (previewLoading) {
            gfx.drawText(tr, "Calculating\u2026",
                    pvX + PREV_PAD, pvY + PREV_HEADER + 14, COL_TEXT_DIM, false);
            return;
        }

        ItemScore s = ItemSelector.lastBest;

        if (s == null) {
            gfx.drawText(tr, "No item scored yet.",
                    pvX + PREV_PAD, pvY + PREV_HEADER + 14, COL_TEXT_DIM, false);
            gfx.drawText(tr, "Start the bot to see results.",
                    pvX + PREV_PAD, pvY + PREV_HEADER + 26, COL_TEXT_DIM, false);
            return;
        }

        int cy = pvY + PREV_HEADER + 10;

        // ── Item name ─────────────────────────────────────────────────────────
        gfx.drawText(tr, s.displayName, pvX + PREV_PAD, cy, COL_TEXT, false);
        cy += 12;
        gfx.drawText(tr, s.productId, pvX + PREV_PAD, cy, COL_TEXT_DIM, false);
        cy += 16;
        gfx.fill(pvX + PREV_PAD, cy, pvX + pvW - PREV_PAD, cy + 1, COL_DIVIDER);
        cy += 8;

        // ── Price rows ────────────────────────────────────────────────────────
        previewRow(gfx, tr, pvX, cy, "Ask price",
                fmtCoinsD(s.askPrice), COL_RED);
        cy += PREV_ROW;
        previewRow(gfx, tr, pvX, cy, "Bid price",
                fmtCoinsD(s.bidPrice), COL_GREEN);
        cy += PREV_ROW;

        gfx.fill(pvX + PREV_PAD, cy, pvX + pvW - PREV_PAD, cy + 1, COL_DIVIDER);
        cy += 8;

        // ── Spread ────────────────────────────────────────────────────────────
        previewRow(gfx, tr, pvX, cy, "Spread",
                fmtCoinsD(s.spread) + "  (" + String.format("%.1f", s.spreadPercent * 100) + "%)",
                COL_YELLOW);
        cy += PREV_ROW;

        gfx.fill(pvX + PREV_PAD, cy, pvX + pvW - PREV_PAD, cy + 1, COL_DIVIDER);
        cy += 8;

        // ── Volume / velocity ─────────────────────────────────────────────────
        double sellsPerHr = Math.min(s.buyMovingWeek, s.sellMovingWeek)
                / s.askPrice / ItemScore.HOURS_PER_WEEK;
        previewRow(gfx, tr, pvX, cy, "Sells/hr",
                String.format("%.0f", sellsPerHr), COL_TEXT);
        cy += PREV_ROW;
        previewRow(gfx, tr, pvX, cy, "Buy vol/wk",
                fmtCoinsD(s.buyMovingWeek), COL_TEXT);
        cy += PREV_ROW;
        previewRow(gfx, tr, pvX, cy, "Sell vol/wk",
                fmtCoinsD(s.sellMovingWeek), COL_TEXT);
        cy += PREV_ROW;

        gfx.fill(pvX + PREV_PAD, cy, pvX + pvW - PREV_PAD, cy + 1, COL_DIVIDER);
        cy += 8;

        // ── Profit ────────────────────────────────────────────────────────────
        previewRow(gfx, tr, pvX, cy, "Profit/hr",
                fmtCoinsD(s.weeklyProfit), COL_GREEN);
        cy += PREV_ROW;
        previewRow(gfx, tr, pvX, cy, "Total profit",
                fmtCoinsD(s.totalProfit), COL_GREEN);
        cy += PREV_ROW;
        previewRow(gfx, tr, pvX, cy, "Amount",
                String.valueOf(s.purchasableAmount), COL_TEXT);
        cy += PREV_ROW;

        // ── Status badges ─────────────────────────────────────────────────────
        cy += 4;
        gfx.fill(pvX + PREV_PAD, cy, pvX + pvW - PREV_PAD, cy + 1, COL_DIVIDER);
        cy += 8;

        if (s.manipulated) {
            gfx.fill(pvX + PREV_PAD, cy - 2, pvX + pvW - PREV_PAD, cy + 10, 0x44FF4455);
            gfx.drawText(tr, "\u26A0 POSSIBLY MANIPULATED",
                    pvX + PREV_PAD, cy, COL_RED, false);
            cy += PREV_ROW;
        }

        if (ModConfig.get().debugMode) {
            gfx.fill(pvX + PREV_PAD, cy - 2, pvX + pvW - PREV_PAD, cy + 10, 0x44FFDD44);
            gfx.drawText(tr, "\u26A0 DEBUG — buying 1 item",
                    pvX + PREV_PAD, cy, COL_YELLOW, false);
        }
    }

    /** Renders a dim label on the left and a coloured value on the right. */
    private void previewRow(DrawContext gfx, net.minecraft.client.font.TextRenderer tr,
            int pvX, int y, String label, String value, int valueCol) {
        gfx.drawText(tr, label, pvX + PREV_PAD, y, COL_TEXT_DIM, false);
        int vw = tr.getWidth(value);
        gfx.drawText(tr, value, pvX + PREV_W - PREV_PAD - vw, y, valueCol, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Compact int coin format: 500K, 1.2M */
    private static String fmtCoins(int coins) {
        if (coins >= 1_000_000)
            return String.format("%.1fM", coins / 1_000_000.0);
        if (coins >= 1_000)
            return (coins / 1_000) + "K";
        return String.valueOf(coins);
    }

    /** Compact double coin format for ItemScore fields */
    private static String fmtCoinsD(double coins) {
        if (coins >= 1_000_000)
            return String.format("%.2fM", coins / 1_000_000.0);
        if (coins >= 1_000)
            return String.format("%.1fK", coins / 1_000.0);
        return String.format("%.0f", coins);
    }

    private ButtonWidget toggle(String label, boolean on, int x, int y,
            ButtonWidget.PressAction action) {
        return ButtonWidget.builder(Text.literal(label + (on ? " \u25CF" : " \u25CB")), action)
                .dimensions(x, y, TOG_W, TOG_H).build();
    }

    private ButtonWidget smallBtn(String label, int x, int y, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), btn -> action.run())
                .dimensions(x, y, 14, 14).build();
    }

    private ButtonWidget smallBtn(String label, int x, int y, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, 14, 14).build();
    }

    private ButtonWidget styledButton(String label, int x, int y, int w, int h,
            ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(Text.literal(label), onPress)
                .dimensions(x, y, w, h).build();
    }
}