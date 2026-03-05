package at.fuji.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class FujiScreen extends Screen {

    private static final int DEFAULT_W = 640;
    private static final int DEFAULT_H = 380;
    private static final int MIN_W = 420;
    private static final int MIN_H = 260;
    private static final int SIDEBAR_W = 110;
    private static final int TOP_BAR_H = 36;
    private static final int RESIZE_ZONE = 12;

    private static final int COL_BG = 0xFF0A0A0F;
    private static final int COL_PANEL = 0xFF12121A;
    private static final int COL_SIDEBAR = 0xFF0D0D16;
    private static final int COL_BORDER = 0xFF2A2A3F;
    private static final int COL_ACCENT = 0xFFB044FF;
    private static final int COL_TEXT_DIM = 0xFF606075;
    private static final int COL_SEL_BG = 0xFF1A1A28;

    private int sx, sy;
    private int screenW = DEFAULT_W;
    private int screenH = DEFAULT_H;

    private boolean dragging = false;
    private boolean resizing = false;
    private int dragOffsetX, dragOffsetY;

    private final List<Sidebar> categories = new ArrayList<>();
    private int selectedCategory = 0;
    private final List<ButtonWidget> sidebarButtons = new ArrayList<>();

    public FujiScreen() {
        super(Text.literal("Fuji"));
        categories.add(new TrackingPanel());
        categories.add(new BazaarPanel());
    }

    public void addWidget(ClickableWidget widget) {
        this.addDrawableChild(widget);
    }

    public void removeWidget(ClickableWidget widget) {
        this.remove((Element) widget);
    }

    @Override
    protected void init() {
        if (sx == 0 && sy == 0) {
            sx = (this.width - screenW) / 2;
            sy = (this.height - screenH) / 2;
        }
        rebuildAll();
    }

    private void rebuildAll() {
        this.clearChildren();
        sidebarButtons.clear();

        for (int i = 0; i < categories.size(); i++) {
            final int idx = i;
            Sidebar cat = categories.get(i);
            int btnY = sy + TOP_BAR_H + 12 + i * 28;
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal(cat.getLabel()),
                    b -> {
                        categories.get(selectedCategory).clearWidgets();
                        selectedCategory = idx;
                        rebuildAll();
                    })
                    .dimensions(sx + 6, btnY, SIDEBAR_W - 12, 20)
                    .build();
            sidebarButtons.add(btn);
            this.addDrawableChild(btn);
        }

        int contentX = sx + SIDEBAR_W;
        int contentY = sy + TOP_BAR_H;
        int contentW = screenW - SIDEBAR_W;
        int contentH = screenH - TOP_BAR_H;
        categories.get(selectedCategory).init(this, contentX, contentY, contentW, contentH);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();

        // Delegate custom hit-areas (colour swatches, etc.) first
        if (categories.get(selectedCategory).onMouseClicked(mouseX, mouseY))
            return true;

        if (isInResizeZone(mouseX, mouseY)) {
            resizing = true;
            return true;
        }
        if (mouseY >= sy && mouseY <= sy + TOP_BAR_H
                && mouseX >= sx && mouseX <= sx + screenW) {
            dragging = true;
            dragOffsetX = (int) mouseX - sx;
            dragOffsetY = (int) mouseY - sy;
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragging = false;
        resizing = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (dragging) {
            sx = (int) mouseX - dragOffsetX;
            sy = (int) mouseY - dragOffsetY;
            clampToScreen();
            rebuildAll();
            return true;
        }
        if (resizing) {
            screenW = Math.max(MIN_W, (int) (mouseX - sx));
            screenH = Math.max(MIN_H, (int) (mouseY - sy));
            clampToScreen();
            rebuildAll();
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (categories.get(selectedCategory).mouseScrolled(mx, my, v))
            return true;
        return super.mouseScrolled(mx, my, h, v);
    }

    private boolean isInResizeZone(double mx, double my) {
        return mx >= sx + screenW - RESIZE_ZONE && my >= sy + screenH - RESIZE_ZONE;
    }

    private void clampToScreen() {
        sx = Math.max(0, Math.min(sx, this.width - screenW));
        sy = Math.max(0, Math.min(sy, this.height - screenH));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, this.width, this.height, 0xBB050508);
        gfx.fill(sx + 5, sy + 5, sx + screenW + 5, sy + screenH + 5, 0x44000000);
        gfx.fill(sx, sy, sx + screenW, sy + screenH, COL_PANEL);
        gfx.fill(sx, sy, sx + screenW, sy + TOP_BAR_H, COL_BG);
        gfx.fill(sx, sy + TOP_BAR_H - 1, sx + screenW, sy + TOP_BAR_H, COL_BORDER);
        gfx.fill(sx, sy, sx + screenW, sy + 2, COL_ACCENT);
        gfx.drawText(this.textRenderer, "FUJI", sx + 12, sy + (TOP_BAR_H - 8) / 2, COL_ACCENT, false);
        gfx.drawText(this.textRenderer, "//  " + categories.get(selectedCategory).getLabel(),
                sx + 42, sy + (TOP_BAR_H - 8) / 2, COL_TEXT_DIM, false);
        gfx.fill(sx, sy + TOP_BAR_H, sx + SIDEBAR_W, sy + screenH, COL_SIDEBAR);
        gfx.fill(sx + SIDEBAR_W - 1, sy + TOP_BAR_H, sx + SIDEBAR_W, sy + screenH, COL_BORDER);
        for (int i = 0; i < categories.size(); i++) {
            int btnY = sy + TOP_BAR_H + 12 + i * 28;
            if (i == selectedCategory) {
                gfx.fill(sx, btnY - 2, sx + SIDEBAR_W - 1, btnY + 22, COL_SEL_BG);
                gfx.fill(sx, btnY - 2, sx + 2, btnY + 22, categories.get(i).getAccentColor());
            }
        }
        gfx.fill(sx + screenW - 8, sy + screenH - 2, sx + screenW, sy + screenH, COL_ACCENT);
        categories.get(selectedCategory).render(gfx, mouseX, mouseY, delta);
        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}