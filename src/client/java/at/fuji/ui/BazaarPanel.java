package at.fuji.ui;

import at.fuji.ModConfig;
import at.fuji.bazaar.BazaarWorker;
import at.fuji.bazaar.ItemScore;
import at.fuji.bazaar.ItemSelector;

import java.util.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BazaarPanel implements Sidebar {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COL_PANEL = 0xFF12121A;
    private static final int COL_ROW_EVEN = 0xFF15151F;
    private static final int COL_ROW_ODD = 0xFF12121A;
    private static final int COL_ROW_HOV = 0xFF1E1E2E;
    private static final int COL_ROW_SEL = 0xFF1A1A3A;
    private static final int COL_BORDER = 0xFF2A2A3F;
    private static final int COL_ACCENT = 0xFFFF6644;
    private static final int COL_RED = 0xFFFF4455;
    private static final int COL_GREEN = 0xFF44FF88;
    private static final int COL_YELLOW = 0xFFFFDD44;
    private static final int COL_CYAN = 0xFF44DDFF;
    private static final int COL_TEXT = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_SCROLLBAR = 0xFF3A3A5A;
    private static final int COL_SCROLL_TH = 0xFF6060A0;
    private static final int COL_SETTINGS = 0xFF0D0D16;
    private static final int COL_COL_HEAD = 0xFF0A0A12;
    private static final int COL_DETAIL_BG = 0xFF0F0F1A;
    private static final int COL_DETAIL_HD = 0xFF0A0A0F;
    private static final int COL_DIVIDER = 0xFF222233;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int HEADER_H = 32;
    private static final int SETTINGS_H = 68; // 3 rows: row1=6..20, row2=26..40, row3=46..60 + 8 pad
    private static final int TAB_H = 22;
    private static final int COL_HDR_H = 16;
    private static final int SEARCH_H = 20; // search bar in rejected tab
    private static final int ROW_H = 20;
    private static final int FOOTER_H = 36;
    private static final int PADDING = 14;
    private static final int SB_W = 4;
    private static final int TOG_W = 68;
    private static final int TOG_H = 14;

    // Detail panel
    private static final int DET_W = 220;
    private static final int DET_GAP = 6;
    private static final int DET_PAD = 10;
    private static final int DET_ROW = 15;
    private static final int DET_HDR = 26;

    // ── Settings x-positions ─────────────────────────────────────────────────
    // Row 1: [NPC SELL] [DEBUG] | Sells/hr [-] val [+] | Vol/wk [-] val [+]
    private static final int R1_NPC_X = PADDING;
    private static final int R1_DBG_X = PADDING + 72;
    private static final int R1_SLB_X = PADDING + 136;
    private static final int R1_SMN_X = PADDING + 190;
    private static final int R1_SV_X = PADDING + 206; // value rendered here
    private static final int R1_SPL_X = PADDING + 246;
    private static final int R1_VLB_X = PADDING + 268;
    private static final int R1_VMN_X = PADDING + 306;
    private static final int R1_VV_X = PADDING + 322; // value rendered here
    private static final int R1_VPL_X = PADDING + 368;

    // Row 2: [●] Profit/hr [-] val [+] | [●] Total profit [-] val [+]
    private static final int R2_PEN_X = PADDING;
    private static final int R2_PLB_X = PADDING + 18;
    private static final int R2_PMN_X = PADDING + 78;
    private static final int R2_PV_X = PADDING + 94;
    private static final int R2_PPL_X = PADDING + 138;
    private static final int R2_TEN_X = PADDING + 162;
    private static final int R2_TLB_X = PADDING + 180;
    private static final int R2_TMN_X = PADDING + 256;
    private static final int R2_TV_X = PADDING + 272;
    private static final int R2_TPL_X = PADDING + 316;

    // Row 3: Margin [-] val [+] | Min sell orders [-] val [+]
    private static final int R3_MLB_X = PADDING;
    private static final int R3_MMN_X = PADDING + 54;
    private static final int R3_MV_X = PADDING + 70;
    private static final int R3_MPL_X = PADDING + 116;
    private static final int R3_OLB_X = PADDING + 140;
    private static final int R3_OMN_X = PADDING + 208;
    private static final int R3_OV_X = PADDING + 224;
    private static final int R3_OPL_X = PADDING + 268;

    // ── Enums ─────────────────────────────────────────────────────────────────
    private enum Tab {
        TOP, REJECTED, BLACKLIST
    }

    private enum SortMode {
        PROFIT_HR("Profit/hr"), TOTAL("Total"), FILL("Fill/hr"), SPREAD("Spread%");

        final String label;

        SortMode(String l) {
            label = l;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private FujiScreen parent;
    private int px, py, pw, ph;

    private Tab tab = Tab.TOP;
    private SortMode sort = SortMode.PROFIT_HR;
    private int scroll = 0;
    private int selectedIdx = -1;
    private ItemScore selectedItem = null;
    private List<ItemScore> sortedList = new ArrayList<>();
    private boolean loading = false;
    private boolean addRowVisible = false;

    // Search state (rejected tab)
    private String rejSearch = "";
    private TextFieldWidget searchBox = null;

    private TextFieldWidget nameBox = null;
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
    public void init(FujiScreen parent, int px, int py, int pw, int ph) {
        this.parent = parent;
        this.px = px;
        this.py = py;
        this.pw = pw;
        this.ph = ph;
        scroll = 0;
        selectedIdx = -1;
        selectedItem = null;
        rebuildSortedList();
        rebuildWidgets();
    }

    @Override
    public void clearWidgets() {
        managedWidgets.forEach(w -> {
            if (w instanceof net.minecraft.client.gui.widget.ClickableWidget cw)
                parent.removeWidget(cw);
        });
        managedWidgets.clear();
        searchBox = null;
        nameBox = null;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (mx < px || mx > px + pw || my < listTop() || my > listBot())
            return false;
        scroll -= (int) (amount * ROW_H);
        clampScroll();
        rebuildWidgets();
        return true;
    }

    // No @Override — Sidebar interface default does nothing; FujiScreen calls it if
    // present
    public boolean onMouseClicked(double mx, double my) {
        if (tab == Tab.TOP) {
            int colHdrY = listTop();
            if (my >= colHdrY && my < colHdrY + COL_HDR_H) {
                if (mx >= profitCol() && mx < totalCol()) {
                    setSortMode(SortMode.PROFIT_HR);
                    return true;
                } else if (mx >= totalCol() && mx < fillCol()) {
                    setSortMode(SortMode.TOTAL);
                    return true;
                } else if (mx >= fillCol() && mx < spreadCol()) {
                    setSortMode(SortMode.FILL);
                    return true;
                } else if (mx >= spreadCol() && mx < btnCol()) {
                    setSortMode(SortMode.SPREAD);
                    return true;
                }
            }
            int rowsTop = colHdrY + COL_HDR_H;
            if (mx >= px && mx < px + pw - SB_W && my >= rowsTop && my < listBot()) {
                int idx = (int) ((my - rowsTop + scroll) / ROW_H);
                if (idx >= 0 && idx < sortedList.size()) {
                    selectedIdx = idx;
                    selectedItem = sortedList.get(idx);
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return false;
    }

    // ── Column x positions (right-anchored) ───────────────────────────────────
    private int btnCol() {
        return px + pw - PADDING - SB_W - 20;
    }

    private int spreadCol() {
        return btnCol() - 56;
    }

    private int fillCol() {
        return spreadCol() - 58;
    }

    private int totalCol() {
        return fillCol() - 68;
    }

    private int profitCol() {
        return totalCol() - 72;
    }

    private int nameCol() {
        return px + PADDING + 22;
    }

    // ── Layout helpers ────────────────────────────────────────────────────────
    private int listTop() {
        int base = py + HEADER_H + SETTINGS_H + TAB_H;
        return (tab == Tab.REJECTED) ? base + SEARCH_H : base;
    }

    private int listBot() {
        return (tab == Tab.BLACKLIST) ? py + ph - FOOTER_H : py + ph;
    }

    private int listCount() {
        return switch (tab) {
            case TOP -> sortedList.size();
            case REJECTED -> filteredRejected().size();
            case BLACKLIST -> ModConfig.get().bazaarBlacklist.size();
        };
    }

    private int maxScroll() {
        int h = listBot() - listTop() - COL_HDR_H;
        return Math.max(0, listCount() * ROW_H - h);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private List<ItemSelector.RejectedItem> filteredRejected() {
        ItemSelector.ScoringResult r = ItemSelector.lastResult;
        if (r == null)
            return Collections.emptyList();
        if (rejSearch.isEmpty())
            return r.rejected;
        String q = rejSearch.toLowerCase();
        return r.rejected.stream()
                .filter(ri -> ri.displayName.toLowerCase().contains(q)
                        || ri.productId.toLowerCase().contains(q)
                        || ri.reason.toLowerCase().contains(q))
                .toList();
    }

    private void setSortMode(SortMode m) {
        sort = m;
        scroll = 0;
        selectedIdx = -1;
        selectedItem = null;
        rebuildSortedList();
        rebuildWidgets();
    }

    private void rebuildSortedList() {
        ItemSelector.ScoringResult result = ItemSelector.lastResult;
        if (result == null) {
            sortedList = new ArrayList<>();
            return;
        }
        sortedList = new ArrayList<>(result.candidates);
        Comparator<ItemScore> cmp = switch (sort) {
            case PROFIT_HR -> Comparator.comparingDouble(s -> -s.weeklyProfit);
            case TOTAL -> Comparator.comparingDouble(s -> -s.totalProfit);
            case FILL -> Comparator.comparingDouble(s -> -s.fillRatePerHour);
            case SPREAD -> Comparator.comparingDouble(s -> -s.spreadPercent);
        };
        sortedList.sort(cmp);
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private void addBtn(ButtonWidget b) {
        managedWidgets.add(b);
        parent.addWidget(b);
    }

    private void addBox(TextFieldWidget b) {
        managedWidgets.add(b);
        parent.addWidget(b);
    }

    private ButtonWidget btn(String l, int x, int y, int w, int h, ButtonWidget.PressAction a) {
        return ButtonWidget.builder(Text.literal(l), a).dimensions(x, y, w, h).build();
    }

    private ButtonWidget tog(String l, boolean on, int x, int y, int w, ButtonWidget.PressAction a) {
        return ButtonWidget.builder(Text.literal(l + (on ? " \u25CF" : " \u25CB")), a)
                .dimensions(x, y, w, TOG_H).build();
    }

    private ButtonWidget sm(String l, int x, int y, Runnable r) {
        return ButtonWidget.builder(Text.literal(l), b -> r.run()).dimensions(x, y, 14, 14).build();
    }

    private ButtonWidget enTog(boolean on, int x, int y, ButtonWidget.PressAction a) {
        return ButtonWidget.builder(Text.literal(on ? "\u25CF" : "\u25CB"), a)
                .dimensions(x, y, 14, 14).build();
    }

    // ── Rebuild ───────────────────────────────────────────────────────────────

    private void rebuildWidgets() {
        clearWidgets();
        clampScroll();
        ModConfig cfg = ModConfig.get();
        int sY = py + HEADER_H;

        // ── Header: REFRESH ───────────────────────────────────────────────
        addBtn(btn(loading ? "..." : "\u21BB REFRESH",
                px + pw - PADDING - 84, py + (HEADER_H - 14) / 2, 84, 14, b -> {
                    if (loading)
                        return;
                    loading = true;
                    selectedIdx = -1;
                    selectedItem = null;
                    rebuildWidgets();
                    ItemSelector.scoreAllItems(BazaarWorker.lastKnownPurse).thenAccept(result -> {
                        loading = false;
                        MinecraftClient.getInstance().execute(() -> {
                            rebuildSortedList();
                            rebuildWidgets();
                        });
                    });
                }));

        // ── Settings Row 1: mode toggles | Sells/hr | Vol/wk ─────────────
        int r1Y = sY + 6;
        addBtn(tog("NPC SELL", cfg.npcSellMode, px + R1_NPC_X, r1Y, TOG_W, b -> {
            cfg.npcSellMode = !cfg.npcSellMode;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(tog("DEBUG", cfg.debugMode, px + R1_DBG_X, r1Y, 56, b -> {
            cfg.debugMode = !cfg.debugMode;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("-", px + R1_SMN_X, r1Y, () -> {
            cfg.minSellsPerHour = Math.max(0, cfg.minSellsPerHour - 10);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("+", px + R1_SPL_X, r1Y, () -> {
            cfg.minSellsPerHour += 10;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("-", px + R1_VMN_X, r1Y, () -> {
            cfg.minWeeklyVolume = Math.max(0, cfg.minWeeklyVolume - 100_000);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("+", px + R1_VPL_X, r1Y, () -> {
            cfg.minWeeklyVolume += 100_000;
            ModConfig.save();
            rebuildWidgets();
        }));

        // ── Settings Row 2: [●] Profit/hr | [●] Total profit ─────────────
        int r2Y = sY + 26;
        addBtn(enTog(cfg.minProfitPerHourEnabled, px + R2_PEN_X, r2Y, b -> {
            cfg.minProfitPerHourEnabled = !cfg.minProfitPerHourEnabled;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("-", px + R2_PMN_X, r2Y, () -> {
            cfg.minProfitPerHour = Math.max(0, cfg.minProfitPerHour - 5_000);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("+", px + R2_PPL_X, r2Y, () -> {
            cfg.minProfitPerHour += 5_000;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(enTog(cfg.minTotalProfitEnabled, px + R2_TEN_X, r2Y, b -> {
            cfg.minTotalProfitEnabled = !cfg.minTotalProfitEnabled;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("-", px + R2_TMN_X, r2Y, () -> {
            cfg.minTotalProfit = Math.max(0, cfg.minTotalProfit - 10_000);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("+", px + R2_TPL_X, r2Y, () -> {
            cfg.minTotalProfit += 10_000;
            ModConfig.save();
            rebuildWidgets();
        }));

        // ── Settings Row 3: Margin | Min sell orders ──────────────────────
        int r3Y = sY + 46;
        addBtn(sm("-", px + R3_MMN_X, r3Y, () -> {
            cfg.minMargin = Math.max(0, cfg.minMargin - 50);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("+", px + R3_MPL_X, r3Y, () -> {
            cfg.minMargin += 50;
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("-", px + R3_OMN_X, r3Y, () -> {
            cfg.minSellOrders = Math.max(0, cfg.minSellOrders - 1);
            ModConfig.save();
            rebuildWidgets();
        }));
        addBtn(sm("+", px + R3_OPL_X, r3Y, () -> {
            cfg.minSellOrders += 1;
            ModConfig.save();
            rebuildWidgets();
        }));

        // ── Tab buttons ───────────────────────────────────────────────────
        int tabY = py + HEADER_H + SETTINGS_H + 4;
        addBtn(btn("TOP ITEMS", px + PADDING, tabY, 80, 14, b -> switchTab(Tab.TOP)));
        addBtn(btn("REJECTED", px + PADDING + 82, tabY, 80, 14, b -> switchTab(Tab.REJECTED)));
        addBtn(btn("BLACKLIST", px + PADDING + 82 * 2, tabY, 80, 14, b -> switchTab(Tab.BLACKLIST)));

        // ── Search bar (rejected tab only) ────────────────────────────────
        if (tab == Tab.REJECTED) {
            int sbY = py + HEADER_H + SETTINGS_H + TAB_H + 2;
            searchBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                    px + PADDING, sbY, pw - PADDING * 2 - SB_W, 16, Text.empty());
            searchBox.setPlaceholder(Text.literal("Search name / product ID / reason\u2026"));
            searchBox.setMaxLength(64);
            searchBox.setText(rejSearch);
            searchBox.setChangedListener(s -> {
                rejSearch = s;
                clampScroll();
                rebuildWidgets();
            });
            addBox(searchBox);
        }

        // ── Content: row-level buttons ────────────────────────────────────
        int rowsTop = listTop() + COL_HDR_H;
        int listBot = listBot();

        if (tab == Tab.TOP) {
            for (int i = 0; i < sortedList.size(); i++) {
                final ItemScore item = sortedList.get(i);
                int rowY = rowsTop + i * ROW_H - scroll;
                if (rowY + ROW_H <= rowsTop || rowY >= listBot)
                    continue;
                addBtn(btn("\u25BA", btnCol() + 1, rowY + (ROW_H - 12) / 2, 18, 12, b -> {
                    if (BazaarWorker.isEnabled())
                        BazaarWorker.stop();
                    BazaarWorker.startWithItem(item);
                    rebuildWidgets();
                }));
            }

        } else if (tab == Tab.BLACKLIST) {
            List<String> bl = cfg.bazaarBlacklist;
            for (int i = 0; i < bl.size(); i++) {
                final int idx = i;
                int rowY = rowsTop + i * ROW_H - scroll;
                if (rowY + ROW_H <= rowsTop || rowY >= listBot)
                    continue;
                addBtn(btn("REMOVE", px + pw - PADDING - SB_W - 46,
                        rowY + (ROW_H - 12) / 2, 44, 12, b -> {
                            cfg.bazaarBlacklist.remove(idx);
                            ModConfig.save();
                            rebuildWidgets();
                        }));
            }
            // Footer
            int footerY = py + ph - FOOTER_H;
            if (addRowVisible) {
                nameBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                        px + PADDING, footerY + 10, 200, 16, Text.empty());
                nameBox.setPlaceholder(Text.literal("Product ID e.g. ENCHANTED_SUGAR"));
                nameBox.setMaxLength(64);
                addBox(nameBox);
                addBtn(btn("ADD", px + PADDING + 208, footerY + 11, 36, 14, b -> {
                    String id = nameBox.getText().trim().toUpperCase().replace(" ", "_");
                    if (!id.isEmpty() && !cfg.bazaarBlacklist.contains(id)) {
                        cfg.bazaarBlacklist.add(id);
                        ModConfig.save();
                    }
                    addRowVisible = false;
                    rebuildWidgets();
                }));
                addBtn(btn("CANCEL", px + PADDING + 250, footerY + 11, 48, 14, b -> {
                    addRowVisible = false;
                    rebuildWidgets();
                }));
            } else {
                addBtn(btn("+ BLACKLIST ITEM", px + PADDING, footerY + 11, 110, 14, b -> {
                    addRowVisible = true;
                    rebuildWidgets();
                }));
            }
        }

        // ── Detail panel widgets ──────────────────────────────────────────
        if (selectedItem != null) {
            int pvX = px + pw + DET_GAP;
            addBtn(btn("\u00D7", pvX + DET_W - DET_PAD - 14, py + 6, 14, 14, b -> {
                selectedIdx = -1;
                selectedItem = null;
                rebuildWidgets();
            }));
            String botLbl = BazaarWorker.isEnabled() ? "\u25A0 STOP BOT" : "\u25BA START BOT";
            addBtn(btn(botLbl, pvX + DET_PAD, py + ph - 28,
                    DET_W - DET_PAD * 2, 16, b -> {
                        if (BazaarWorker.isEnabled())
                            BazaarWorker.stop();
                        else
                            BazaarWorker.startWithItem(selectedItem);
                        rebuildWidgets();
                    }));
        }
    }

    private void switchTab(Tab t) {
        tab = t;
        scroll = 0;
        if (t != Tab.TOP) {
            selectedIdx = -1;
            selectedItem = null;
        }
        rebuildWidgets();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        var tr = MinecraftClient.getInstance().textRenderer;
        var cfg = ModConfig.get();

        renderHeader(gfx, tr);
        renderSettings(gfx, tr, cfg);
        renderTabBar(gfx, tr);

        switch (tab) {
            case TOP -> renderTopItems(gfx, tr, mouseX, mouseY);
            case REJECTED -> renderRejected(gfx, tr, mouseX, mouseY);
            case BLACKLIST -> renderBlacklist(gfx, tr, mouseX, mouseY, cfg);
        }

        renderScrollbar(gfx);

        if (tab == Tab.BLACKLIST)
            gfx.fill(px, py + ph - FOOTER_H, px + pw, py + ph - FOOTER_H + 1, COL_BORDER);

        if (selectedItem != null)
            renderDetail(gfx, tr);
    }

    private void renderHeader(DrawContext gfx, TextRenderer tr) {
        gfx.fill(px, py, px + pw, py + HEADER_H, 0xFF0A0A0F);
        gfx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_BORDER);
        gfx.drawText(tr, "BAZAAR", px + PADDING, py + (HEADER_H - 8) / 2, COL_ACCENT, false);
        boolean on = BazaarWorker.isEnabled();
        gfx.drawText(tr, on ? "BOT ON  " + BazaarWorker.itemName : "BOT OFF",
                px + PADDING + 56, py + (HEADER_H - 8) / 2,
                on ? COL_GREEN : COL_RED, false);
    }

    private void renderSettings(DrawContext gfx, TextRenderer tr, ModConfig cfg) {
        int sY = py + HEADER_H;
        gfx.fill(px, sY, px + pw, sY + SETTINGS_H, COL_SETTINGS);
        gfx.fill(px, sY + SETTINGS_H - 1, px + pw, sY + SETTINGS_H, COL_BORDER);

        // Row 1 text
        int r1Y = sY + 9;
        gfx.drawText(tr, "Sells/hr", px + R1_SLB_X, r1Y, COL_TEXT_DIM, false);
        centeredValue(gfx, tr, cfg.minSellsPerHour + "/hr", px + R1_SMN_X, px + R1_SPL_X, r1Y, COL_TEXT);
        gfx.drawText(tr, "Vol/wk", px + R1_VLB_X, r1Y, COL_TEXT_DIM, false);
        centeredValue(gfx, tr, fmtD(cfg.minWeeklyVolume), px + R1_VMN_X, px + R1_VPL_X, r1Y, COL_TEXT);

        // Row 2 text
        int r2Y = sY + 29;
        int phC = cfg.minProfitPerHourEnabled ? COL_TEXT_DIM : 0xFF404050;
        int tpC = cfg.minTotalProfitEnabled ? COL_TEXT_DIM : 0xFF404050;
        gfx.drawText(tr, "Profit/hr", px + R2_PLB_X, r2Y, phC, false);
        centeredValue(gfx, tr, fmtD(cfg.minProfitPerHour), px + R2_PMN_X, px + R2_PPL_X, r2Y,
                cfg.minProfitPerHourEnabled ? COL_GREEN : 0xFF404050);
        gfx.drawText(tr, "Total profit", px + R2_TLB_X, r2Y, tpC, false);
        centeredValue(gfx, tr, fmtD(cfg.minTotalProfit), px + R2_TMN_X, px + R2_TPL_X, r2Y,
                cfg.minTotalProfitEnabled ? COL_CYAN : 0xFF404050);

        // Row 3 text
        int r3Y = sY + 49;
        gfx.drawText(tr, "Margin", px + R3_MLB_X, r3Y, COL_TEXT_DIM, false);
        centeredValue(gfx, tr, fmtD(cfg.minMargin), px + R3_MMN_X, px + R3_MPL_X, r3Y, COL_YELLOW);
        gfx.drawText(tr, "Min sell orders", px + R3_OLB_X, r3Y, COL_TEXT_DIM, false);
        centeredValue(gfx, tr, String.valueOf(cfg.minSellOrders), px + R3_OMN_X, px + R3_OPL_X, r3Y, COL_YELLOW);
    }

    /**
     * Renders value text centered in the pixel gap between a [-] and [+] button
     * pair.
     */
    private void centeredValue(DrawContext gfx, TextRenderer tr, String val,
            int minBtnX, int plusBtnX, int y, int col) {
        int gapStart = minBtnX + 14 + 2;
        int gapEnd = plusBtnX - 2;
        int vw = tr.getWidth(val);
        int vx = gapStart + Math.max(0, (gapEnd - gapStart - vw) / 2);
        gfx.drawText(tr, val, vx, y, col, false);
    }

    private void renderTabBar(DrawContext gfx, TextRenderer tr) {
        int tbY = py + HEADER_H + SETTINGS_H;
        gfx.fill(px, tbY, px + pw, tbY + TAB_H, 0xFF0A0A12);
        gfx.fill(px, tbY + TAB_H - 1, px + pw, tbY + TAB_H, COL_BORDER);
        int activeX = px + PADDING + tab.ordinal() * 82;
        gfx.fill(activeX, tbY + TAB_H - 2, activeX + 80, tbY + TAB_H, COL_ACCENT);

        ItemSelector.ScoringResult r = ItemSelector.lastResult;
        int topCount = sortedList.size();
        int rejCount = r != null ? r.rejected.size() : 0;
        gfx.drawText(tr, "(" + topCount + ")",
                px + PADDING + 56, tbY + (TAB_H - 8) / 2, COL_TEXT_DIM, false);
        gfx.drawText(tr, "(" + rejCount + ")",
                px + PADDING + 82 + 54, tbY + (TAB_H - 8) / 2, COL_TEXT_DIM, false);
    }

    private void renderTopItems(DrawContext gfx, TextRenderer tr, int mouseX, int mouseY) {
        int listTop = listTop();
        int rowsTop = listTop + COL_HDR_H;
        int listBot = listBot();

        gfx.fill(px, listTop, px + pw, listTop + COL_HDR_H, COL_COL_HEAD);
        gfx.drawText(tr, "#", px + PADDING, listTop + 4, COL_TEXT_DIM, false);
        gfx.drawText(tr, "NAME", nameCol(), listTop + 4, COL_TEXT_DIM, false);
        sortHdr(gfx, tr, "Profit/hr", profitCol(), listTop, sort == SortMode.PROFIT_HR);
        sortHdr(gfx, tr, "Total", totalCol(), listTop, sort == SortMode.TOTAL);
        sortHdr(gfx, tr, "Fill/hr", fillCol(), listTop, sort == SortMode.FILL);
        sortHdr(gfx, tr, "Spread%", spreadCol(), listTop, sort == SortMode.SPREAD);
        gfx.fill(px, listTop + COL_HDR_H - 1, px + pw, listTop + COL_HDR_H, COL_BORDER);

        if (loading) {
            gfx.drawText(tr, "Scoring items\u2026", px + PADDING, rowsTop + 10, COL_TEXT_DIM, false);
            return;
        }
        if (sortedList.isEmpty()) {
            gfx.drawText(tr,
                    ItemSelector.lastResult == null
                            ? "Press  \u21BB REFRESH  to score items"
                            : "No items pass current filters",
                    px + PADDING, rowsTop + 10, COL_TEXT_DIM, false);
            return;
        }

        gfx.enableScissor(px, rowsTop, px + pw - SB_W, listBot);
        for (int i = 0; i < sortedList.size(); i++) {
            ItemScore s = sortedList.get(i);
            int rowY = rowsTop + i * ROW_H - scroll;
            if (rowY + ROW_H <= rowsTop || rowY >= listBot)
                continue;

            boolean hov = mouseX >= px && mouseX < px + pw - SB_W
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean sel = i == selectedIdx;
            gfx.fill(px, rowY, px + pw - SB_W, rowY + ROW_H,
                    sel ? COL_ROW_SEL : hov ? COL_ROW_HOV : (i % 2 == 0 ? COL_ROW_EVEN : COL_ROW_ODD));

            boolean isActive = BazaarWorker.isEnabled()
                    && s.displayName.equals(BazaarWorker.itemName);
            gfx.fill(px, rowY, px + 2, rowY + ROW_H, isActive ? COL_GREEN : COL_BORDER);

            int ty = rowY + (ROW_H - 8) / 2;
            gfx.drawText(tr, String.valueOf(i + 1), px + PADDING, ty, COL_TEXT_DIM, false);
            gfx.drawText(tr, trunc(tr, s.displayName, profitCol() - nameCol() - 4), nameCol(), ty, COL_TEXT, false);
            gfx.drawText(tr, fmtD(s.weeklyProfit), profitCol(), ty, COL_GREEN, false);
            gfx.drawText(tr, fmtD(s.totalProfit), totalCol(), ty, COL_CYAN, false);
            gfx.drawText(tr, String.format("%.0f", s.fillRatePerHour), fillCol(), ty, COL_TEXT, false);
            gfx.drawText(tr, String.format("%.1f%%", s.spreadPercent * 100), spreadCol(), ty, COL_YELLOW, false);

            gfx.fill(px, rowY + ROW_H - 1, px + pw - SB_W, rowY + ROW_H, COL_BORDER);
        }
        gfx.disableScissor();
    }

    private void renderRejected(DrawContext gfx, TextRenderer tr, int mouseX, int mouseY) {
        // Search bar background
        int sbY = py + HEADER_H + SETTINGS_H + TAB_H;
        gfx.fill(px, sbY, px + pw, sbY + SEARCH_H, 0xFF0A0A10);
        gfx.fill(px, sbY + SEARCH_H - 1, px + pw, sbY + SEARCH_H, COL_BORDER);

        int listTop = listTop();
        int rowsTop = listTop + COL_HDR_H;
        int listBot = listBot();

        gfx.fill(px, listTop, px + pw, listTop + COL_HDR_H, COL_COL_HEAD);
        gfx.drawText(tr, "NAME", px + PADDING, listTop + 4, COL_TEXT_DIM, false);
        gfx.drawText(tr, "ASK", px + PADDING + 196, listTop + 4, COL_TEXT_DIM, false);
        gfx.drawText(tr, "REASON", px + PADDING + 248, listTop + 4, COL_TEXT_DIM, false);
        gfx.fill(px, listTop + COL_HDR_H - 1, px + pw, listTop + COL_HDR_H, COL_BORDER);

        ItemSelector.ScoringResult result = ItemSelector.lastResult;
        if (result == null) {
            gfx.drawText(tr, "Press  \u21BB REFRESH  to score items",
                    px + PADDING, rowsTop + 10, COL_TEXT_DIM, false);
            return;
        }

        List<ItemSelector.RejectedItem> list = filteredRejected();
        if (list.isEmpty() && !rejSearch.isEmpty()) {
            gfx.drawText(tr, "No results for \"" + rejSearch + "\"",
                    px + PADDING, rowsTop + 10, COL_TEXT_DIM, false);
            return;
        }

        gfx.enableScissor(px, rowsTop, px + pw - SB_W, listBot);
        for (int i = 0; i < list.size(); i++) {
            ItemSelector.RejectedItem r = list.get(i);
            int rowY = rowsTop + i * ROW_H - scroll;
            if (rowY + ROW_H <= rowsTop || rowY >= listBot)
                continue;

            boolean hov = mouseX >= px && mouseX < px + pw - SB_W
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            gfx.fill(px, rowY, px + pw - SB_W, rowY + ROW_H,
                    hov ? COL_ROW_HOV : (i % 2 == 0 ? COL_ROW_EVEN : COL_ROW_ODD));
            gfx.fill(px, rowY, px + 2, rowY + ROW_H, COL_RED);

            int ty = rowY + (ROW_H - 8) / 2;
            gfx.drawText(tr, trunc(tr, r.displayName, 186), px + PADDING, ty, COL_TEXT_DIM, false);
            gfx.drawText(tr, r.askPrice > 0 ? fmtD(r.askPrice) : "N/A", px + PADDING + 196, ty, COL_TEXT_DIM, false);
            gfx.drawText(tr, r.reason, px + PADDING + 248, ty, COL_RED, false);
            gfx.fill(px, rowY + ROW_H - 1, px + pw - SB_W, rowY + ROW_H, COL_BORDER);
        }
        gfx.disableScissor();

        // Result count
        if (!rejSearch.isEmpty()) {
            gfx.drawText(tr, list.size() + " / " + result.rejected.size() + " shown",
                    px + pw - PADDING - SB_W - 80, sbY + 6, COL_TEXT_DIM, false);
        }
    }

    private void renderBlacklist(DrawContext gfx, TextRenderer tr,
            int mouseX, int mouseY, ModConfig cfg) {
        int listTop = listTop();
        int rowsTop = listTop + COL_HDR_H;
        int listBot = listBot();
        var bl = cfg.bazaarBlacklist;

        gfx.fill(px, listTop, px + pw, listTop + COL_HDR_H, COL_COL_HEAD);
        gfx.drawText(tr, "PRODUCT ID  (" + bl.size() + ")",
                px + PADDING, listTop + 4, COL_TEXT_DIM, false);
        gfx.fill(px, listTop + COL_HDR_H - 1, px + pw, listTop + COL_HDR_H, COL_BORDER);

        gfx.enableScissor(px, rowsTop, px + pw - SB_W, listBot);
        for (int i = 0; i < bl.size(); i++) {
            int rowY = rowsTop + i * ROW_H - scroll;
            if (rowY + ROW_H <= rowsTop || rowY >= listBot)
                continue;
            boolean hov = mouseX >= px && mouseX < px + pw - SB_W
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            gfx.fill(px, rowY, px + pw - SB_W, rowY + ROW_H,
                    hov ? COL_ROW_HOV : (i % 2 == 0 ? COL_ROW_EVEN : COL_ROW_ODD));
            gfx.fill(px, rowY, px + 2, rowY + ROW_H, COL_RED);
            gfx.drawText(tr, bl.get(i), px + PADDING, rowY + (ROW_H - 8) / 2, COL_TEXT, false);
            gfx.fill(px, rowY + ROW_H - 1, px + pw - SB_W, rowY + ROW_H, COL_BORDER);
        }
        gfx.disableScissor();
    }

    private void renderScrollbar(DrawContext gfx) {
        int sbX = px + pw - SB_W;
        int listTop = listTop() + COL_HDR_H;
        int listBot = listBot();
        int vpH = listBot - listTop;
        int contentH = listCount() * ROW_H;
        gfx.fill(sbX, listTop, sbX + SB_W, listBot, COL_SCROLLBAR);
        if (contentH > vpH) {
            int thumbH = Math.max(16, vpH * vpH / contentH);
            int thumbY = listTop + (maxScroll() > 0 ? (vpH - thumbH) * scroll / maxScroll() : 0);
            gfx.fill(sbX, thumbY, sbX + SB_W, thumbY + thumbH, COL_SCROLL_TH);
        }
    }

    private void renderDetail(DrawContext gfx, TextRenderer tr) {
        ItemScore s = selectedItem;
        int pvX = px + pw + DET_GAP, pvY = py, pvW = DET_W, pvH = ph;

        gfx.fill(pvX, pvY, pvX + pvW, pvY + pvH, COL_DETAIL_BG);
        gfx.fill(pvX, pvY, pvX + 1, pvY + pvH, COL_BORDER);
        gfx.fill(pvX + pvW - 1, pvY, pvX + pvW, pvY + pvH, COL_BORDER);
        gfx.fill(pvX, pvY, pvX + pvW, pvY + 1, COL_BORDER);
        gfx.fill(pvX, pvY + pvH - 1, pvX + pvW, pvY + pvH, COL_BORDER);
        gfx.fill(pvX, pvY, pvX + pvW, pvY + DET_HDR, COL_DETAIL_HD);
        gfx.fill(pvX, pvY + DET_HDR - 1, pvX + pvW, pvY + DET_HDR, COL_BORDER);
        gfx.drawText(tr, "ITEM DETAIL", pvX + DET_PAD, pvY + (DET_HDR - 8) / 2, COL_ACCENT, false);

        int cy = pvY + DET_HDR + 8;
        gfx.drawText(tr, trunc(tr, s.displayName, pvW - DET_PAD * 2 - 16), pvX + DET_PAD, cy, COL_TEXT, false);
        cy += 11;
        gfx.drawText(tr, s.productId, pvX + DET_PAD, cy, COL_TEXT_DIM, false);
        cy += 13;
        div(gfx, pvX, pvW, cy);
        cy += 6;

        drow(gfx, tr, pvX, pvW, cy, "Ask price", fmtD(s.askPrice), COL_RED);
        cy += DET_ROW;
        drow(gfx, tr, pvX, pvW, cy, "Bid price", fmtD(s.bidPrice), COL_GREEN);
        cy += DET_ROW;
        int nc = s.npcSellPrice > s.askPrice ? COL_GREEN : COL_TEXT_DIM;
        drow(gfx, tr, pvX, pvW, cy, "NPC sell",
                s.npcSellPrice > 0 ? fmtD(s.npcSellPrice) : "N/A", nc);
        cy += DET_ROW;
        div(gfx, pvX, pvW, cy);
        cy += 6;

        drow(gfx, tr, pvX, pvW, cy, "Spread",
                fmtD(s.spread) + " (" + String.format("%.1f", s.spreadPercent * 100) + "%)",
                COL_YELLOW);
        cy += DET_ROW;
        div(gfx, pvX, pvW, cy);
        cy += 6;

        drow(gfx, tr, pvX, pvW, cy, "Fill/hr", String.format("%.0f", s.fillRatePerHour), COL_TEXT);
        cy += DET_ROW;
        drow(gfx, tr, pvX, pvW, cy, "Buy vol/wk", fmtD(s.buyMovingWeek), COL_TEXT);
        cy += DET_ROW;
        drow(gfx, tr, pvX, pvW, cy, "Sell vol/wk", fmtD(s.sellMovingWeek), COL_TEXT);
        cy += DET_ROW;
        div(gfx, pvX, pvW, cy);
        cy += 6;

        drow(gfx, tr, pvX, pvW, cy, "BZ profit/hr", fmtD(s.weeklyProfit), COL_GREEN);
        cy += DET_ROW;
        drow(gfx, tr, pvX, pvW, cy, "BZ total profit", fmtD(s.totalProfit), COL_GREEN);
        cy += DET_ROW;
        drow(gfx, tr, pvX, pvW, cy, "Order amount", String.valueOf(s.purchasableAmount), COL_TEXT);
        cy += DET_ROW;

        if (s.npcSellPrice > s.askPrice) {
            div(gfx, pvX, pvW, cy);
            cy += 6;
            drow(gfx, tr, pvX, pvW, cy, "NPC profit/hr", fmtD(s.npcProfitPerHour), COL_CYAN);
            cy += DET_ROW;
            drow(gfx, tr, pvX, pvW, cy, "NPC per item", fmtD(s.npcProfitPerItem), COL_CYAN);
            cy += DET_ROW;
        }

        if (s.manipulated) {
            cy += 4;
            div(gfx, pvX, pvW, cy);
            cy += 6;
            gfx.fill(pvX + DET_PAD, cy - 2, pvX + pvW - DET_PAD, cy + 10, 0x44FF4455);
            gfx.drawText(tr, "\u26A0 LIKELY MANIPULATED", pvX + DET_PAD, cy, COL_RED, false);
            cy += DET_ROW;
        }
        if (ModConfig.get().debugMode) {
            gfx.fill(pvX + DET_PAD, cy - 2, pvX + pvW - DET_PAD, cy + 10, 0x44FFDD44);
            gfx.drawText(tr, "\u26A0 DEBUG — buying 1 item", pvX + DET_PAD, cy, COL_YELLOW, false);
        }
    }

    // ── Render helpers ────────────────────────────────────────────────────────

    private void sortHdr(DrawContext gfx, TextRenderer tr, String text, int x, int y, boolean active) {
        gfx.drawText(tr, active ? text + " \u25BE" : text, x, y + 4,
                active ? COL_ACCENT : COL_TEXT_DIM, false);
    }

    private void div(DrawContext gfx, int pvX, int pvW, int y) {
        gfx.fill(pvX + DET_PAD, y, pvX + pvW - DET_PAD, y + 1, COL_DIVIDER);
    }

    private void drow(DrawContext gfx, TextRenderer tr, int pvX, int pvW,
            int y, String label, String value, int col) {
        gfx.drawText(tr, label, pvX + DET_PAD, y, COL_TEXT_DIM, false);
        gfx.drawText(tr, value, pvX + pvW - DET_PAD - tr.getWidth(value), y, col, false);
    }

    // ── Format helpers ────────────────────────────────────────────────────────

    private static String fmtD(double v) {
        if (v >= 1_000_000_000)
            return String.format("%.1fB", v / 1_000_000_000.0);
        if (v >= 1_000_000)
            return String.format("%.2fM", v / 1_000_000.0);
        if (v >= 1_000)
            return String.format("%.1fK", v / 1_000.0);
        return String.format("%.0f", v);
    }

    private static String trunc(TextRenderer tr, String text, int maxW) {
        if (tr.getWidth(text) <= maxW)
            return text;
        while (!text.isEmpty() && tr.getWidth(text + "\u2026") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "\u2026";
    }
}