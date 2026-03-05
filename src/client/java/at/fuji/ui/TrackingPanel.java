package at.fuji.ui;

import at.fuji.target.TargetConfig;
import at.fuji.target.TargetManager;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TrackingPanel implements Sidebar {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COL_PANEL    = 0xFF12121A;
    private static final int COL_ROW_EVEN = 0xFF15151F;
    private static final int COL_ROW_HOV  = 0xFF1E1E2E;
    private static final int COL_EXPAND   = 0xFF0F0F18;
    private static final int COL_BORDER   = 0xFF2A2A3F;
    private static final int COL_ACCENT   = 0xFFB044FF;
    private static final int COL_GREEN    = 0xFF44FF88;
    private static final int COL_RED      = 0xFFFF4455;
    private static final int COL_TEXT     = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_CYAN     = 0xFF44DDFF;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 40;
    private static final int ROW_H    = 28;
    private static final int EXP_H    = 78;
    private static final int PADDING  = 14;

    // Colour palette (ARGB)
    private static final int[] SWATCH_COLORS = {
            0xFFB044FF, // Purple
            0xFF44DDFF, // Cyan
            0xFFFF4455, // Red
            0xFF44FF88, // Green
            0xFFFFDD44, // Yellow
            0xFFFFFFFF, // White
            0xFFFF8844, // Orange
            0xFF4488FF, // Blue
    };
    private static final int SWATCH_W = 16, SWATCH_H = 12, SWATCH_GAP = 4;

    // ── State ─────────────────────────────────────────────────────────────────
    private FujiScreen parent;
    private int px, py, pw, ph;

    private int expandedRow    = -1;
    private int editingNameRow = -1;
    private boolean buildingNewTarget = false;

    private TextFieldWidget nameBox, radiusBox;
    private TextFieldWidget editNameBox;

    private final List<Object> managedWidgets = new ArrayList<>();

    // ── Sidebar contract ──────────────────────────────────────────────────────

    @Override public String getLabel()    { return "TRACKING"; }
    @Override public int getAccentColor() { return COL_ACCENT;  }

    @Override
    public void init(FujiScreen parent, int panelX, int panelY, int panelW, int panelH) {
        this.parent = parent;
        this.px = panelX; this.py = panelY;
        this.pw = panelW; this.ph = panelH;
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

    // ── Row Y helper ──────────────────────────────────────────────────────────

    private int getRowY(int i) {
        int y = py + HEADER_H;
        for (int j = 0; j < i; j++) {
            y += ROW_H;
            if (expandedRow == j) y += EXP_H;
        }
        return y;
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private void addBtn(ButtonWidget btn) { managedWidgets.add(btn); parent.addWidget(btn); }
    private void addBox(TextFieldWidget b) { managedWidgets.add(b); parent.addWidget(b); }

    private ButtonWidget btn(String label, int x, int y, int w, int h, ButtonWidget.PressAction a) {
        return ButtonWidget.builder(Text.literal(label), a).dimensions(x, y, w, h).build();
    }
    private ButtonWidget tog(String label, boolean on, int x, int y, int w, ButtonWidget.PressAction a) {
        return ButtonWidget.builder(Text.literal(label + (on ? " \u25CF" : " \u25CB")), a)
                .dimensions(x, y, w, 14).build();
    }

    // ── Rebuild ───────────────────────────────────────────────────────────────

    public void rebuildWidgets() {
        clearWidgets();
        List<TargetConfig> targets = TargetManager.targets;

        for (int i = 0; i < targets.size(); i++) {
            final int idx = i;
            TargetConfig cfg = targets.get(i);
            int rowY = getRowY(i);
            int midY = rowY + (ROW_H - 14) / 2;

            // Expand / collapse
            String arrow = (expandedRow == i) ? "\u25BC" : "\u25BA";
            addBtn(btn(arrow, px + PADDING, midY, 14, 14, b -> {
                expandedRow = (expandedRow == idx) ? -1 : idx;
                if (editingNameRow == idx) editingNameRow = -1;
                rebuildWidgets();
            }));

            // WP / TR toggles (always visible)
            int wpX = px + pw - PADDING - 110;
            int trX = wpX + 56;
            addBtn(tog("WP", cfg.waypointEnabled, wpX, midY, 52, b -> {
                cfg.waypointEnabled = !cfg.waypointEnabled;
                TargetManager.saveConfig();
                b.setMessage(Text.literal("WP" + (cfg.waypointEnabled ? " \u25CF" : " \u25CB")));
            }));
            addBtn(tog("TR", cfg.tracerEnabled, trX, midY, 52, b -> {
                cfg.tracerEnabled = !cfg.tracerEnabled;
                TargetManager.saveConfig();
                b.setMessage(Text.literal("TR" + (cfg.tracerEnabled ? " \u25CF" : " \u25CB")));
            }));

            // ── EXPANDED SECTION ──────────────────────────────────────────
            if (expandedRow == i) {
                int expY = rowY + ROW_H;

                // Row B (y = expY+28): ENABLED / ALERT / SHOW DIST
                int r2Y = expY + 28;
                addBtn(tog("ENABLED",  cfg.enabled,             px + PADDING,       r2Y, 68, b -> {
                    cfg.enabled = !cfg.enabled; TargetManager.saveConfig(); rebuildWidgets(); }));
                addBtn(tog("ALERT",    cfg.alertEnabled,        px + PADDING + 74,  r2Y, 56, b -> {
                    cfg.alertEnabled = !cfg.alertEnabled; TargetManager.saveConfig(); rebuildWidgets(); }));
                addBtn(tog("DIST",     cfg.showDistanceEnabled, px + PADDING + 136, r2Y, 48, b -> {
                    cfg.showDistanceEnabled = !cfg.showDistanceEnabled; TargetManager.saveConfig(); rebuildWidgets(); }));

                // Row C (y = expY+52): radius controls / name editor / REMOVE
                int r3Y = expY + 52;
                if (editingNameRow == i) {
                    editNameBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                            px + PADDING, r3Y - 1, 160, 14, Text.empty());
                    editNameBox.setText(cfg.mobName);
                    editNameBox.setMaxLength(32);
                    addBox(editNameBox);
                    addBtn(btn("\u2714", px + PADDING + 166, r3Y, 18, 14, b -> {
                        String text = editNameBox.getText().trim();
                        if (!text.isEmpty()) { cfg.mobName = text; TargetManager.saveConfig(); }
                        editingNameRow = -1; rebuildWidgets();
                    }));
                    addBtn(btn("\u2716", px + PADDING + 188, r3Y, 18, 14, b -> {
                        editingNameRow = -1; rebuildWidgets();
                    }));
                } else {
                    // Layout: "Radius:" text | [-] | value (text) | [+] | RENAME
                    // "Radius:" rendered in render() at px+PADDING
                    addBtn(btn("-", px + PADDING + 50, r3Y, 14, 14, b -> {
                        cfg.radius = Math.max(10, cfg.radius - 10);
                        TargetManager.saveConfig(); rebuildWidgets();
                    }));
                    // radius value text rendered between buttons in render()
                    addBtn(btn("+", px + PADDING + 106, r3Y, 14, 14, b -> {
                        cfg.radius = Math.min(2000, cfg.radius + 10);
                        TargetManager.saveConfig(); rebuildWidgets();
                    }));
                    addBtn(btn("RENAME", px + PADDING + 126, r3Y, 46, 14, b -> {
                        editingNameRow = idx; rebuildWidgets();
                    }));
                }

                addBtn(btn("REMOVE", px + pw - PADDING - 44, r3Y, 44, 14, b -> {
                    TargetManager.removeTarget(cfg.mobName);
                    if (expandedRow == idx) expandedRow = -1;
                    if (editingNameRow == idx) editingNameRow = -1;
                    rebuildWidgets();
                }));
            }
        }

        // ── Footer: add new target ─────────────────────────────────────────
        int footerY = py + ph - FOOTER_H;
        int lastRowBottom = getRowY(targets.size());
        if (lastRowBottom > footerY) footerY = lastRowBottom + 4;

        if (!buildingNewTarget) {
            addBtn(btn("+ NEW TARGET", px + PADDING, footerY + 13, 90, 14, b -> {
                buildingNewTarget = true; rebuildWidgets();
            }));
        } else {
            nameBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                    px + PADDING, footerY + 12, 140, 16, Text.empty());
            nameBox.setPlaceholder(Text.literal("Mob name..."));
            nameBox.setMaxLength(32);
            addBox(nameBox);

            radiusBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                    px + PADDING + 148, footerY + 12, 60, 16, Text.empty());
            radiusBox.setPlaceholder(Text.literal("Radius..."));
            radiusBox.setText("150");
            radiusBox.setMaxLength(5);
            addBox(radiusBox);

            addBtn(btn("ADD", px + PADDING + 216, footerY + 13, 36, 14, b -> {
                String name = nameBox.getText().trim();
                if (!name.isEmpty()) {
                    float r = 150;
                    try { r = Float.parseFloat(radiusBox.getText()); } catch (NumberFormatException ignored) {}
                    TargetManager.addTarget(name, true, true, r);
                }
                buildingNewTarget = false; rebuildWidgets();
            }));
            addBtn(btn("CANCEL", px + PADDING + 258, footerY + 13, 48, 14, b -> {
                buildingNewTarget = false; rebuildWidgets();
            }));
        }
    }

    // ── Colour swatch clicks ──────────────────────────────────────────────────

    @Override
    public boolean onMouseClicked(double mx, double my) {
        if (expandedRow < 0 || expandedRow >= TargetManager.targets.size()) return false;
        TargetConfig cfg = TargetManager.targets.get(expandedRow);
        int expY = getRowY(expandedRow) + ROW_H;
        int swatchStartX = px + PADDING + 46;
        int swatchY = expY + 6;
        for (int c = 0; c < SWATCH_COLORS.length; c++) {
            int sx = swatchStartX + c * (SWATCH_W + SWATCH_GAP);
            if (mx >= sx && mx < sx + SWATCH_W && my >= swatchY && my < swatchY + SWATCH_H) {
                cfg.color = SWATCH_COLORS[c];
                TargetManager.saveConfig();
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        var tr  = MinecraftClient.getInstance().textRenderer;
        var mc  = MinecraftClient.getInstance();

        // Header
        gfx.fill(px, py, px + pw, py + HEADER_H, 0xFF0A0A0F);
        gfx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_BORDER);
        gfx.drawText(tr, "TRACKING", px + PADDING, py + (HEADER_H - 8) / 2, COL_ACCENT, false);
        gfx.drawText(tr, TargetManager.targets.size() + " TARGETS",
                px + PADDING + 70, py + (HEADER_H - 8) / 2, COL_TEXT_DIM, false);

        // Column hints
        int chY = py + HEADER_H + 2;
        gfx.drawText(tr, "MOB", px + PADDING + 18, chY, COL_TEXT_DIM, false);
        gfx.drawText(tr, "WP / TR", px + pw - PADDING - 110, chY, COL_TEXT_DIM, false);

        List<TargetConfig> targets = TargetManager.targets;

        for (int i = 0; i < targets.size(); i++) {
            TargetConfig cfg = targets.get(i);
            int rowY = getRowY(i);

            boolean hovered = mouseX >= px && mouseX < px + pw
                    && mouseY >= rowY && mouseY < rowY + ROW_H;

            gfx.fill(px, rowY, px + pw, rowY + ROW_H,
                    hovered ? COL_ROW_HOV : (i % 2 == 0 ? COL_ROW_EVEN : COL_PANEL));

            if (cfg.enabled)
                gfx.fill(px, rowY, px + 2, rowY + ROW_H, cfg.color);

            int dotCol = (cfg.currentPos != null) ? COL_GREEN : COL_RED;
            gfx.fill(px + PADDING + 18, rowY + ROW_H / 2 - 2,
                     px + PADDING + 23, rowY + ROW_H / 2 + 3, dotCol);

            String label = cfg.mobName.toUpperCase() + (cfg.enabled ? "" : " [OFF]");
            gfx.drawText(tr, label, px + PADDING + 28, rowY + (ROW_H - 8) / 2,
                    cfg.enabled ? COL_TEXT : COL_TEXT_DIM, false);

            gfx.fill(px, rowY + ROW_H - 1, px + pw, rowY + ROW_H, COL_BORDER);

            // ── Expanded section ──────────────────────────────────────────
            if (expandedRow == i) {
                int expY = rowY + ROW_H;
                gfx.fill(px, expY, px + pw, expY + EXP_H, COL_EXPAND);
                gfx.fill(px, expY, px + 2, expY + EXP_H, cfg.color);
                gfx.fill(px, expY + EXP_H - 1, px + pw, expY + EXP_H, COL_BORDER);

                // Row A: colour swatches
                gfx.drawText(tr, "Color:", px + PADDING + 2, expY + 9, COL_TEXT_DIM, false);
                int swatchX = px + PADDING + 46;
                for (int c = 0; c < SWATCH_COLORS.length; c++) {
                    int sx = swatchX + c * (SWATCH_W + SWATCH_GAP);
                    if (cfg.color == SWATCH_COLORS[c])
                        gfx.fill(sx - 2, expY + 4, sx + SWATCH_W + 2, expY + 6 + SWATCH_H + 2, 0xFFFFFFFF);
                    gfx.fill(sx, expY + 6, sx + SWATCH_W, expY + 6 + SWATCH_H, SWATCH_COLORS[c]);
                }

                // Row C: radius label, then [-] value [+]
                if (editingNameRow != i) {
                    // "Radius:" label — left of [-] button (which is at px+PADDING+50)
                    gfx.drawText(tr, "Radius:", px + PADDING, expY + 55, COL_TEXT_DIM, false);
                    // Value centered between [-] (ends at +64) and [+] (starts at +106)
                    String radStr = (int) cfg.radius + "m";
                    int radW = tr.getWidth(radStr);
                    int gapStart = px + PADDING + 64 + 2;
                    int gapEnd   = px + PADDING + 106 - 2;
                    int radX     = gapStart + (gapEnd - gapStart - radW) / 2;
                    gfx.drawText(tr, radStr, radX, expY + 55, COL_CYAN, false);
                }
            }
        }

        // Footer separator
        gfx.fill(px, py + ph - FOOTER_H, px + pw, py + ph - FOOTER_H + 1, COL_BORDER);
    }
}