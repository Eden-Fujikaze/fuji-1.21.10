package at.fuji.ui;

import at.fuji.ModConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BazaarPanel implements Sidebar {

    private static final int COL_PANEL = 0xFF12121A;
    private static final int COL_ROW_EVEN = 0xFF15151F;
    private static final int COL_ROW_HOVER = 0xFF1E1E2E;
    private static final int COL_BORDER = 0xFF2A2A3F;
    private static final int COL_ACCENT = 0xFFFF6644;
    private static final int COL_RED = 0xFFFF4455;
    private static final int COL_GREEN = 0xFF44FF88;
    private static final int COL_TEXT = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_SCROLLBAR = 0xFF3A3A5A;
    private static final int COL_SCROLL_TH = 0xFF6060A0;

    private static final int ROW_HEIGHT = 28;
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 40;
    private static final int PADDING = 14;

    private static final int NPC_BTN_W = 60;
    private static final int NPC_BTN_H = 14;

    /** Width of the scrollbar strip on the right edge. */
    private static final int SB_W = 4;

    private FujiScreen parent;
    private int px, py, pw, ph;

    private int scrollOffset = 0;

    private boolean addRowVisible = false;
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

    /**
     * Forward mouse-wheel events here from FujiScreen:
     * if (activeSidebar instanceof BazaarPanel bp) bp.mouseScrolled(mx, my,
     * amount);
     * Or add a default no-op to the Sidebar interface and call it on all panels.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int listTop = py + HEADER_H;
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
        return ph - HEADER_H - FOOTER_H;
    }

    private int maxScroll() {
        int contentH = ModConfig.get().bazaarBlacklist.size() * ROW_HEIGHT;
        return Math.max(0, contentH - listViewportH());
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private void addBtn(ButtonWidget btn) {
        managedWidgets.add(btn);
        parent.addWidget(btn);
    }

    private void addBox(TextFieldWidget box) {
        managedWidgets.add(box);
        parent.addWidget(box);
    }

    private void rebuildWidgets() {
        clearWidgets();
        clampScroll();

        List<String> blacklist = ModConfig.get().bazaarBlacklist;
        int listTop = py + HEADER_H;
        int listBottom = py + ph - FOOTER_H;

        // NPC Sell toggle
        boolean npcOn = ModConfig.get().npcSellMode;
        addBtn(styledButton(
                npcOn ? "NPC \u25CF" : "NPC \u25CB",
                px + pw - PADDING - NPC_BTN_W,
                py + (HEADER_H - NPC_BTN_H) / 2,
                NPC_BTN_W, NPC_BTN_H,
                btn -> {
                    ModConfig.get().npcSellMode = !ModConfig.get().npcSellMode;
                    ModConfig.save();
                    rebuildWidgets();
                }));

        // REMOVE buttons — only register rows visible in the viewport
        for (int i = 0; i < blacklist.size(); i++) {
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

        // Footer
        int footerY = py + ph - FOOTER_H;
        if (addRowVisible) {
            nameBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                    px + PADDING, footerY + 12, 200, 16, Text.empty());
            nameBox.setPlaceholder(Text.literal("Product ID e.g. ENCHANTED_SUGAR"));
            nameBox.setMaxLength(64);
            addBox(nameBox);

            addBtn(styledButton("ADD", px + PADDING + 208, footerY + 13, 36, 14, btn -> {
                String id = nameBox.getText().trim().toUpperCase().replace(" ", "_");
                if (!id.isEmpty() && !ModConfig.get().bazaarBlacklist.contains(id)) {
                    ModConfig.get().bazaarBlacklist.add(id);
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
        boolean npcOn = ModConfig.get().npcSellMode;
        List<String> blacklist = ModConfig.get().bazaarBlacklist;
        int listTop = py + HEADER_H;
        int listBottom = py + ph - FOOTER_H;
        int vpH = listViewportH();
        int contentH = blacklist.size() * ROW_HEIGHT;

        // Header (outside scissor — always visible)
        gfx.fill(px, py, px + pw, py + HEADER_H, 0xFF0A0A0F);
        gfx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_BORDER);
        gfx.drawText(MinecraftClient.getInstance().textRenderer, "BAZAAR BLACKLIST",
                px + PADDING, py + (HEADER_H - 8) / 2, COL_ACCENT, false);
        gfx.drawText(MinecraftClient.getInstance().textRenderer,
                blacklist.size() + " ITEMS",
                px + PADDING + 110, py + (HEADER_H - 8) / 2, COL_TEXT_DIM, false);

        int npcLabelX = px + pw - PADDING - NPC_BTN_W - 6
                - MinecraftClient.getInstance().textRenderer.getWidth("NPC SELL");
        gfx.drawText(MinecraftClient.getInstance().textRenderer, "NPC SELL",
                npcLabelX, py + (HEADER_H - 8) / 2,
                npcOn ? COL_GREEN : COL_TEXT_DIM, false);

        // Scrollable list (scissored)
        gfx.enableScissor(px, listTop, px + pw - SB_W, listBottom);

        gfx.drawText(MinecraftClient.getInstance().textRenderer, "PRODUCT ID",
                px + PADDING, listTop + 2 - scrollOffset, COL_TEXT_DIM, false);

        for (int i = 0; i < blacklist.size(); i++) {
            int rowY = listTop + i * ROW_HEIGHT - scrollOffset;
            if (rowY + ROW_HEIGHT <= listTop || rowY >= listBottom)
                continue;

            boolean hovered = mouseX >= px && mouseX < px + pw - SB_W
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            gfx.fill(px, rowY, px + pw - SB_W, rowY + ROW_HEIGHT,
                    hovered ? COL_ROW_HOVER : (i % 2 == 0 ? COL_ROW_EVEN : COL_PANEL));
            gfx.fill(px, rowY, px + 2, rowY + ROW_HEIGHT, COL_RED);
            gfx.drawText(MinecraftClient.getInstance().textRenderer,
                    blacklist.get(i),
                    px + PADDING, rowY + (ROW_HEIGHT - 8) / 2, COL_TEXT, false);
            gfx.fill(px, rowY + ROW_HEIGHT - 1, px + pw - SB_W, rowY + ROW_HEIGHT, COL_BORDER);
        }

        gfx.disableScissor();

        // Scrollbar
        int sbX = px + pw - SB_W;
        gfx.fill(sbX, listTop, sbX + SB_W, listBottom, COL_SCROLLBAR);
        if (contentH > vpH) {
            int thumbH = Math.max(16, vpH * vpH / contentH);
            int thumbRange = vpH - thumbH;
            int thumbY = listTop + (maxScroll() > 0 ? thumbRange * scrollOffset / maxScroll() : 0);
            gfx.fill(sbX, thumbY, sbX + SB_W, thumbY + thumbH, COL_SCROLL_TH);
        }

        // Footer separator
        gfx.fill(px, py + ph - FOOTER_H, px + pw, py + ph - FOOTER_H + 1, COL_BORDER);
    }

    private ButtonWidget styledButton(String label, int x, int y, int w, int h,
            ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(Text.literal(label), onPress).dimensions(x, y, w, h).build();
    }
}