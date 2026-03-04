package at.fuji.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
    private static final int COL_TEXT_DIM = 0xFF606075;
    private static final int COL_SEL_BG = 0xFF1A1A28;

    private int sx, sy;

    private final List<Sidebar> categories = new ArrayList<>();
    private int selectedCategory = 0;
    private final List<ButtonWidget> sidebarButtons = new ArrayList<>();

    public FujiScreen() {
        super(Text.literal("Fuji"));
        categories.add(new TrackingPanel());
        categories.add(new BazaarPanel());
    }

    public void addWidget(net.minecraft.client.gui.widget.ClickableWidget widget) {
        this.addDrawableChild(widget);
    }

    public void removeWidget(net.minecraft.client.gui.widget.ClickableWidget widget) {
        this.remove((net.minecraft.client.gui.Element) widget);
    }

    @Override
    protected void init() {
        sx = (this.width - SCREEN_WIDTH) / 2;
        sy = (this.height - SCREEN_HEIGHT) / 2;
        rebuildAll();
    }

    private void rebuildAll() {
        this.clearChildren();
        sidebarButtons.clear();

        for (int i = 0; i < categories.size(); i++) {
            final int idx = i;
            Sidebar cat = categories.get(i);
            int btnY = sy + TOP_BAR_H + 12 + i * 28;

            ButtonWidget btn = ButtonWidget.builder(Text.literal(cat.getLabel()), b -> {
                categories.get(selectedCategory).clearWidgets();
                selectedCategory = idx;
                rebuildAll();
            }).dimensions(sx + 6, btnY, SIDEBAR_W - 12, 20).build();

            sidebarButtons.add(btn);
            this.addDrawableChild(btn);
        }

        int contentX = sx + SIDEBAR_W;
        int contentY = sy + TOP_BAR_H;
        int contentW = SCREEN_WIDTH - SIDEBAR_W;
        int contentH = SCREEN_HEIGHT - TOP_BAR_H;
        categories.get(selectedCategory).init(this, contentX, contentY, contentW, contentH);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (categories.get(selectedCategory).mouseScrolled(mouseX, mouseY, verticalAmount))
            return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, this.width, this.height, 0xBB050508);
        gfx.fill(sx + 5, sy + 5, sx + SCREEN_WIDTH + 5, sy + SCREEN_HEIGHT + 5, 0x44000000);
        gfx.fill(sx, sy, sx + SCREEN_WIDTH, sy + SCREEN_HEIGHT, COL_PANEL);
        gfx.fill(sx, sy, sx + SCREEN_WIDTH, sy + TOP_BAR_H, COL_BG);
        gfx.fill(sx, sy + TOP_BAR_H - 1, sx + SCREEN_WIDTH, sy + TOP_BAR_H, COL_BORDER);
        gfx.fill(sx, sy, sx + SCREEN_WIDTH, sy + 2, COL_ACCENT);
        gfx.drawText(this.textRenderer, "FUJI", sx + 12, sy + (TOP_BAR_H - 8) / 2, COL_ACCENT, false);
        gfx.drawText(this.textRenderer, "//  " + categories.get(selectedCategory).getLabel(),
                sx + 42, sy + (TOP_BAR_H - 8) / 2, COL_TEXT_DIM, false);
        gfx.fill(sx, sy + TOP_BAR_H, sx + SIDEBAR_W, sy + SCREEN_HEIGHT, COL_SIDEBAR);
        gfx.fill(sx + SIDEBAR_W - 1, sy + TOP_BAR_H, sx + SIDEBAR_W, sy + SCREEN_HEIGHT, COL_BORDER);

        for (int i = 0; i < categories.size(); i++) {
            int btnY = sy + TOP_BAR_H + 12 + i * 28;
            if (i == selectedCategory) {
                gfx.fill(sx, btnY - 2, sx + SIDEBAR_W - 1, btnY + 22, COL_SEL_BG);
                gfx.fill(sx, btnY - 2, sx + 2, btnY + 22, categories.get(i).getAccentColor());
            }
        }

        drawBorder(gfx, sx, sy, SCREEN_WIDTH, SCREEN_HEIGHT, COL_BORDER);
        drawCornerAccent(gfx, sx, sy, 1);
        drawCornerAccent(gfx, sx + SCREEN_WIDTH, sy, -1);
        categories.get(selectedCategory).render(gfx, mouseX, mouseY, delta);
        super.render(gfx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x, y, x + w, y + 1, color);
        gfx.fill(x, y + h - 1, x + w, y + h, color);
        gfx.fill(x, y, x + 1, y + h, color);
        gfx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCornerAccent(DrawContext gfx, int x, int y, int dir) {
        gfx.fill(x, y - 4, x + dir * 8, y - 3, COL_ACCENT);
        gfx.fill(x - 1, y - 4, x, y + 4, COL_ACCENT);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}