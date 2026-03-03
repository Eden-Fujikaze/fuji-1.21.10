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
    private static final int COL_TEXT = 0xFFE0E0F0;
    private static final int COL_TEXT_DIM = 0xFF808099;

    private static final int ROW_HEIGHT = 28;
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 40;
    private static final int PADDING = 14;

    private FujiScreen parent;
    private int px, py, pw, ph;

    private boolean addRowVisible = false;
    private TextFieldWidget nameBox;
    private final List<Object> managedWidgets = new ArrayList<>();

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
        List<String> blacklist = ModConfig.get().bazaarBlacklist;

        for (int i = 0; i < blacklist.size(); i++) {
            final int idx = i;
            int rowY = py + HEADER_H + i * ROW_HEIGHT;
            int btnY = rowY + (ROW_HEIGHT - 14) / 2;

            addBtn(styledButton("REMOVE", px + pw - PADDING - 44, btnY, 44, 14, btn -> {
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

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        // Header
        gfx.fill(px, py, px + pw, py + HEADER_H, 0xFF0A0A0F);
        gfx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_BORDER);
        gfx.drawText(MinecraftClient.getInstance().textRenderer, "BAZAAR BLACKLIST",
                px + PADDING, py + (HEADER_H - 8) / 2, COL_ACCENT, false);
        gfx.drawText(MinecraftClient.getInstance().textRenderer,
                ModConfig.get().bazaarBlacklist.size() + " ITEMS",
                px + PADDING + 110, py + (HEADER_H - 8) / 2, COL_TEXT_DIM, false);

        // Column header
        gfx.drawText(MinecraftClient.getInstance().textRenderer, "PRODUCT ID",
                px + PADDING, py + HEADER_H + 2, COL_TEXT_DIM, false);

        // Rows
        List<String> blacklist = ModConfig.get().bazaarBlacklist;
        for (int i = 0; i < blacklist.size(); i++) {
            int rowY = py + HEADER_H + i * ROW_HEIGHT;
            boolean hovered = mouseX >= px && mouseX <= px + pw
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            gfx.fill(px, rowY, px + pw, rowY + ROW_HEIGHT,
                    hovered ? COL_ROW_HOVER : (i % 2 == 0 ? COL_ROW_EVEN : COL_PANEL));
            gfx.fill(px, rowY, px + 2, rowY + ROW_HEIGHT, COL_RED);

            gfx.drawText(MinecraftClient.getInstance().textRenderer,
                    blacklist.get(i),
                    px + PADDING, rowY + (ROW_HEIGHT - 8) / 2, COL_TEXT, false);

            gfx.fill(px, rowY + ROW_HEIGHT - 1, px + pw, rowY + ROW_HEIGHT, COL_BORDER);
        }

        // Footer separator
        gfx.fill(px, py + ph - FOOTER_H, px + pw, py + ph - FOOTER_H + 1, COL_BORDER);
    }

    private ButtonWidget styledButton(String label, int x, int y, int w, int h,
            ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(Text.literal(label), onPress).dimensions(x, y, w, h).build();
    }
}