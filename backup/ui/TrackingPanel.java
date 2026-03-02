package at.fuji.ui;

import at.fuji.target.TargetConfig;
import at.fuji.target.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TrackingPanel implements Sidebar {

    private static final int COL_PANEL = 0xFF12121A;
    private static final int COL_ROW_EVEN = 0xFF15151F;
    private static final int COL_ROW_HOVER = 0xFF1E1E2E;
    private static final int COL_BORDER = 0xFF2A2A3F;
    private static final int COL_ACCENT = 0xFFB044FF;
    private static final int COL_GREEN = 0xFF44FF88;
    private static final int COL_RED = 0xFFFF4455;
    private static final int COL_TEXT = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_CYAN = 0xFF44DDFF;

    private static final int ROW_HEIGHT = 32;
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 40;
    private static final int PADDING = 14;

    private FujiScreen parent;
    private int px, py, pw, ph;

    private int editingRow = -1;
    private boolean addRowVisible = false;

    private EditBox nameBox, radiusBox;
    private EditBox editNameBox, editRadiusBox;

    private final List<Object> managedWidgets = new ArrayList<>();

    @Override
    public String getLabel() {
        return "TRACKING";
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
        rebuildWidgets();
    }

    @Override
    public void clearWidgets() {
        managedWidgets.forEach(w -> {
            if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw)
                parent.removeWidget(aw);
        });
        managedWidgets.clear();
    }

    private void addBtn(Button btn) {
        managedWidgets.add(btn);
        parent.addWidget(btn);
    }

    private void addBox(EditBox box) {
        managedWidgets.add(box);
        parent.addWidget(box);
    }

    public void rebuildWidgets() {
        clearWidgets();

        List<TargetConfig> targets = TargetManager.targets;

        for (int i = 0; i < targets.size(); i++) {
            final int idx = i;
            TargetConfig cfg = targets.get(i);
            int rowY = py + HEADER_H + i * ROW_HEIGHT;
            int btnY = rowY + (ROW_HEIGHT - 14) / 2;

            if (editingRow == i) {
                editNameBox = new EditBox(Minecraft.getInstance().font,
                        px + PADDING + 12, btnY - 1, 100, 14, Component.empty());
                editNameBox.setValue(cfg.mobName);
                editNameBox.setMaxLength(32);
                addBox(editNameBox);

                editRadiusBox = new EditBox(Minecraft.getInstance().font,
                        px + PADDING + 120, btnY - 1, 48, 14, Component.empty());
                editRadiusBox.setValue(String.valueOf((int) cfg.radius));
                editRadiusBox.setMaxLength(5);
                addBox(editRadiusBox);

                addBtn(styledButton("✔", px + PADDING + 176, btnY, 20, 14, btn -> {
                    cfg.mobName = editNameBox.getValue();
                    try {
                        cfg.radius = Float.parseFloat(editRadiusBox.getValue());
                    } catch (NumberFormatException ignored) {
                    }
                    editingRow = -1;
                    rebuildWidgets();
                }));

                addBtn(styledButton("✖", px + PADDING + 200, btnY, 20, 14, btn -> {
                    editingRow = -1;
                    rebuildWidgets();
                }));

            } else {
                addBtn(styledButton("EDIT", px + PADDING + 12, btnY, 28, 14, btn -> {
                    editingRow = idx;
                    rebuildWidgets();
                }));
            }

            int col2 = px + 220;
            addBtn(styledButton(cfg.waypointEnabled ? "WP ●" : "WP ○", col2, btnY, 52, 14, btn -> {
                TargetManager.toggleWaypoint(cfg.mobName);
                btn.setMessage(Component.literal(cfg.waypointEnabled ? "WP ●" : "WP ○"));
            }));

            int col3 = col2 + 58;
            addBtn(styledButton(cfg.tracerEnabled ? "TR ●" : "TR ○", col3, btnY, 52, 14, btn -> {
                TargetManager.toggleTracer(cfg.mobName);
                btn.setMessage(Component.literal(cfg.tracerEnabled ? "TR ●" : "TR ○"));
            }));

            int col4 = col3 + 150;
            addBtn(styledButton("-", col4, btnY, 14, 14,
                    btn -> {
                        cfg.radius = Math.max(10, cfg.radius - 10);
                        rebuildWidgets();
                    }));
            addBtn(styledButton("+", col4 + 18, btnY, 14, 14,
                    btn -> {
                        cfg.radius += 10;
                        rebuildWidgets();
                    }));

            addBtn(styledButton("REMOVE", px + pw - PADDING - 44, btnY, 44, 14, btn -> {
                TargetManager.removeTarget(cfg.mobName);
                if (editingRow == idx)
                    editingRow = -1;
                rebuildWidgets();
            }));
        }

        // Footer
        int footerY = py + ph - FOOTER_H;
        if (addRowVisible) {
            nameBox = new EditBox(Minecraft.getInstance().font,
                    px + PADDING, footerY + 12, 140, 16, Component.empty());
            nameBox.setHint(Component.literal("Mob name..."));
            nameBox.setMaxLength(32);
            addBox(nameBox);

            radiusBox = new EditBox(Minecraft.getInstance().font,
                    px + PADDING + 148, footerY + 12, 60, 16, Component.empty());
            radiusBox.setHint(Component.literal("Radius..."));
            radiusBox.setValue("150");
            radiusBox.setMaxLength(5);
            addBox(radiusBox);

            addBtn(styledButton("ADD", px + PADDING + 216, footerY + 13, 36, 14, btn -> {
                String name = nameBox.getValue().trim();
                if (!name.isEmpty()) {
                    float radius = 150;
                    try {
                        radius = Float.parseFloat(radiusBox.getValue());
                    } catch (NumberFormatException ignored) {
                    }
                    TargetManager.addTarget(name, true, true, radius);
                }
                addRowVisible = false;
                rebuildWidgets();
            }));

            addBtn(styledButton("CANCEL", px + PADDING + 258, footerY + 13, 48, 14, btn -> {
                addRowVisible = false;
                rebuildWidgets();
            }));

        } else {
            addBtn(styledButton("+ NEW TARGET", px + PADDING, footerY + 13, 90, 14,
                    btn -> {
                        addRowVisible = true;
                        rebuildWidgets();
                    }));
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // Panel header
        gfx.fill(px, py, px + pw, py + HEADER_H, 0xFF0A0A0F);
        gfx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_BORDER);
        gfx.drawString(Minecraft.getInstance().font, "TRACKING",
                px + PADDING, py + (HEADER_H - 8) / 2, COL_ACCENT, false);
        gfx.drawString(Minecraft.getInstance().font,
                TargetManager.targets.size() + " TARGETS",
                px + PADDING + 70, py + (HEADER_H - 8) / 2, COL_TEXT_DIM, false);

        // Column headers
        int chY = py + HEADER_H + 2;
        gfx.drawString(Minecraft.getInstance().font, "MOB", px + PADDING + 44, chY, COL_TEXT_DIM, false);
        gfx.drawString(Minecraft.getInstance().font, "WAYPOINT", px + 220, chY, COL_TEXT_DIM, false);
        gfx.drawString(Minecraft.getInstance().font, "TRACER", px + 278, chY, COL_TEXT_DIM, false);
        gfx.drawString(Minecraft.getInstance().font, "RADIUS", px + 340, chY, COL_TEXT_DIM, false);

        // Rows
        List<TargetConfig> targets = TargetManager.targets;
        for (int i = 0; i < targets.size(); i++) {
            TargetConfig cfg = targets.get(i);
            int rowY = py + HEADER_H + i * ROW_HEIGHT;

            boolean hovered = mouseX >= px && mouseX <= px + pw
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            gfx.fill(px, rowY, px + pw, rowY + ROW_HEIGHT,
                    hovered ? COL_ROW_HOVER : (i % 2 == 0 ? COL_ROW_EVEN : COL_PANEL));

            if (cfg.waypointEnabled || cfg.tracerEnabled)
                gfx.fill(px, rowY, px + 2, rowY + ROW_HEIGHT, COL_ACCENT);

            int dotColor = cfg.currentPos != null ? COL_GREEN : COL_RED;
            gfx.fill(px + PADDING, rowY + ROW_HEIGHT / 2 - 2,
                    px + PADDING + 5, rowY + ROW_HEIGHT / 2 + 3, dotColor);

            if (editingRow != i) {
                gfx.drawString(Minecraft.getInstance().font,
                        cfg.mobName.toUpperCase(),
                        px + PADDING + 44, rowY + (ROW_HEIGHT - 8) / 2, COL_TEXT, false);
                gfx.drawString(Minecraft.getInstance().font,
                        (int) cfg.radius + "m",
                        px + 344, rowY + (ROW_HEIGHT - 8) / 2, COL_CYAN, false);
            }

            gfx.fill(px, rowY + ROW_HEIGHT - 1, px + pw, rowY + ROW_HEIGHT, COL_BORDER);
        }

        // Footer separator
        gfx.fill(px, py + ph - FOOTER_H, px + pw, py + ph - FOOTER_H + 1, COL_BORDER);
    }

    private Button styledButton(String label, int x, int y, int w, int h, Button.OnPress onPress) {
        return Button.builder(Component.literal(label), onPress).bounds(x, y, w, h).build();
    }
}