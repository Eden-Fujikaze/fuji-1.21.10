package at.fuji.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FujiScreen extends Screen {

    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 380;
    private static final int SIDEBAR_W = 110;
    private static final int TOP_BAR_H = 36;

    private static final int COL_BG = 0xFF0A0A0F;
    private static final int COL_PANEL = 0xFF12121A;
    private static final int COL_SIDEBAR = 0xFF0D0D16;
    private static final int COL_BORDER = 0xFF2A2A3F;
    private static final int COL_ACCENT = 0xFFB044FF;
    // private static final int COL_TEXT = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF606075;
    private static final int COL_SEL_BG = 0xFF1A1A28;

    private int sx, sy;

    private final List<Sidebar> categories = new ArrayList<>();
    private int selectedCategory = 0;
    private final List<Button> sidebarButtons = new ArrayList<>();

    public FujiScreen() {
        super(Component.literal("Fuji"));
        categories.add(new TrackingPanel());
        // Future panels:
        // categories.add(new ScoreboardPanel());
        // categories.add(new SettingsPanel());
    }

    public void addWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    public void removeWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.removeWidget((net.minecraft.client.gui.components.events.GuiEventListener) widget);
    }

    @Override
    protected void init() {
        sx = (this.width - SCREEN_WIDTH) / 2;
        sy = (this.height - SCREEN_HEIGHT) / 2;
        rebuildAll();
    }

    private void rebuildAll() {
        this.clearWidgets();
        sidebarButtons.clear();

        // Sidebar buttons
        for (int i = 0; i < categories.size(); i++) {
            final int idx = i;
            Sidebar cat = categories.get(i);
            int btnY = sy + TOP_BAR_H + 12 + i * 28;

            Button btn = Button.builder(Component.literal(cat.getLabel()), b -> {
                categories.get(selectedCategory).clearWidgets();
                selectedCategory = idx;
                rebuildAll();
            }).bounds(sx + 6, btnY, SIDEBAR_W - 12, 20).build();

            sidebarButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        // Init active panel in content area
        int contentX = sx + SIDEBAR_W;
        int contentY = sy + TOP_BAR_H;
        int contentW = SCREEN_WIDTH - SIDEBAR_W;
        int contentH = SCREEN_HEIGHT - TOP_BAR_H;
        categories.get(selectedCategory).init(this, contentX, contentY, contentW, contentH);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // Backdrop
        gfx.fill(0, 0, this.width, this.height, 0xBB050508);

        // Shadow
        gfx.fill(sx + 5, sy + 5, sx + SCREEN_WIDTH + 5, sy + SCREEN_HEIGHT + 5, 0x44000000);

        // Main panel
        gfx.fill(sx, sy, sx + SCREEN_WIDTH, sy + SCREEN_HEIGHT, COL_PANEL);

        // Top bar
        gfx.fill(sx, sy, sx + SCREEN_WIDTH, sy + TOP_BAR_H, COL_BG);
        gfx.fill(sx, sy + TOP_BAR_H - 1, sx + SCREEN_WIDTH, sy + TOP_BAR_H, COL_BORDER);

        // Top accent line
        gfx.fill(sx, sy, sx + SCREEN_WIDTH, sy + 2, COL_ACCENT);

        // Title
        gfx.drawString(this.font, "FUJI", sx + 12, sy + (TOP_BAR_H - 8) / 2, COL_ACCENT, false);
        gfx.drawString(this.font, "//  " + categories.get(selectedCategory).getLabel(),
                sx + 42, sy + (TOP_BAR_H - 8) / 2, COL_TEXT_DIM, false);

        // Sidebar background
        gfx.fill(sx, sy + TOP_BAR_H, sx + SIDEBAR_W, sy + SCREEN_HEIGHT, COL_SIDEBAR);
        gfx.fill(sx + SIDEBAR_W - 1, sy + TOP_BAR_H, sx + SIDEBAR_W, sy + SCREEN_HEIGHT, COL_BORDER);

        // Sidebar entries highlight
        for (int i = 0; i < categories.size(); i++) {
            int btnY = sy + TOP_BAR_H + 12 + i * 28;
            if (i == selectedCategory) {
                gfx.fill(sx, btnY - 2, sx + SIDEBAR_W - 1, btnY + 22, COL_SEL_BG);
                gfx.fill(sx, btnY - 2, sx + 2, btnY + 22, categories.get(i).getAccentColor());
            }
        }

        // Outer border
        drawBorder(gfx, sx, sy, SCREEN_WIDTH, SCREEN_HEIGHT, COL_BORDER);
        drawCornerAccent(gfx, sx, sy, 1);
        drawCornerAccent(gfx, sx + SCREEN_WIDTH, sy, -1);

        // Active category content
        categories.get(selectedCategory).render(gfx, mouseX, mouseY, delta);

        super.render(gfx, mouseX, mouseY, delta);
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x, y, x + w, y + 1, color);
        gfx.fill(x, y + h - 1, x + w, y + h, color);
        gfx.fill(x, y, x + 1, y + h, color);
        gfx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCornerAccent(GuiGraphics gfx, int x, int y, int dir) {
        gfx.fill(x, y - 4, x + dir * 8, y - 3, COL_ACCENT);
        gfx.fill(x - 1, y - 4, x, y + 4, COL_ACCENT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}