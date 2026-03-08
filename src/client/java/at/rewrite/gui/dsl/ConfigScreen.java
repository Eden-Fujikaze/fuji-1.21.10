package at.rewrite.gui.dsl;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private static final int SIDEBAR_W   = 130;
    private static final int PADDING     = 10;
    private static final int ROW_H       = 22;
    private static final int SECTION_GAP = 8;

    private final List<Tab> tabs;
    private int activeTab = 0;
    // Only holds state for EntryLists in the ACTIVE tab — rebuilt on tab switch
    private final List<EntryListState> EntryListStates = new ArrayList<>();

    public ConfigScreen(String title, Tab... tabs) {
        super(Text.literal(title));
        this.tabs = List.of(tabs);
    }

    // ── Init — called on open and on every tab switch ─────────────────────────

    @Override
    protected void init() {
        // Remove all previously registered widgets so old TextFields disappear
        clearChildren();
        EntryListStates.clear();

        // Only create TextFieldWidgets for options in the ACTIVE tab
        for (Option opt : tabs.get(activeTab).options) {
            if (opt instanceof Option.EntryList bl) {
                TextFieldWidget tf = new TextFieldWidget(
                        textRenderer, 0, 0, 10, 16, Text.empty());
                tf.setMaxLength(120);
                tf.setSuggestion("e.g. minecraft:stone");
                addDrawableChild(tf);
                EntryListStates.add(new EntryListState(bl, tf));
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        TextRenderer tr = textRenderer;

        // Transparent dark overlay (not fully opaque)
        ctx.fill(0, 0, width, height, 0x99000000);

        // ── Sidebar ───────────────────────────────────────────────────────────
        ctx.fill(0, 0, SIDEBAR_W, height, 0xCC0F0F17);
        ctx.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, height, 0xFF2A2A3A);
        ctx.fill(0, 0, SIDEBAR_W, 26, 0xCC1A1A26);
        ctx.drawTextWithShadow(tr, getTitle(), PADDING, 9, 0xFFB48EFF);

        int tabY = 32;
        for (int i = 0; i < tabs.size(); i++) {
            boolean sel = i == activeTab;
            boolean hov = mouseX < SIDEBAR_W && mouseY >= tabY && mouseY < tabY + ROW_H;
            ctx.fill(0, tabY, SIDEBAR_W, tabY + ROW_H,
                    sel ? 0xDD8B5CF6 : hov ? 0xBB2A2A3A : 0xBB15151D);
            if (sel) ctx.fill(0, tabY, 3, tabY + ROW_H, 0xFFD0AAFF);
            ctx.drawTextWithShadow(tr, tabs.get(i).name,
                    PADDING + (sel ? 4 : 2), tabY + (ROW_H - 8) / 2, 0xFFFFFFFF);
            tabY += ROW_H + 2;
        }

        // ── Content panel ─────────────────────────────────────────────────────
        ctx.fill(SIDEBAR_W, 0, width, height, 0xBB12121A);
        ctx.fill(SIDEBAR_W, 0, width, 26, 0xCC1A1A26);
        ctx.fill(SIDEBAR_W, 25, width, 26, 0xFF2A2A3A);
        ctx.drawTextWithShadow(tr, tabs.get(activeTab).name, SIDEBAR_W + PADDING, 9, 0xFFCCCCDD);

        // ── Options ───────────────────────────────────────────────────────────
        int optX = SIDEBAR_W + PADDING;
        int optY = 36;
        int optW = width - SIDEBAR_W - PADDING * 2;

        for (Option opt : tabs.get(activeTab).options) {
            switch (opt) {

                case Option.Toggle tog -> {
                    boolean val = tog.get().get();
                    boolean hov = mouseX >= optX && mouseX < optX + optW
                            && mouseY >= optY && mouseY < optY + ROW_H;
                    ctx.fill(optX, optY, optX + optW, optY + ROW_H, hov ? 0xBB1E1E2E : 0xBB15151E);
                    ctx.drawTextWithShadow(tr, tog.label(), optX + 6, optY + (ROW_H - 8) / 2, 0xFFCCCCDD);
                    renderPill(ctx, optX + optW - 38, optY + (ROW_H - 14) / 2, val);
                    optY += ROW_H + 2;
                }

                case Option.Hotkey hk -> {
                    String keyText = hk.getKeyName().get();
                    ctx.fill(optX, optY, optX + optW, optY + ROW_H, 0xBB15151E);
                    ctx.drawTextWithShadow(tr, hk.label(), optX + 6, optY + (ROW_H - 8) / 2, 0xFFCCCCDD);
                    int kw = tr.getWidth(keyText) + 12;
                    ctx.fill(optX + optW - kw - 2, optY + 3, optX + optW - 2, optY + ROW_H - 3, 0xFF2A2A3A);
                    ctx.drawTextWithShadow(tr, keyText,
                            optX + optW - kw + 4, optY + (ROW_H - 8) / 2, 0xFFFFFFFF);
                    optY += ROW_H + 2;
                }

                case Option.NumInput ni -> {
                    float val = ni.get().get();
                    String valStr = (val == (int) val)
                            ? String.valueOf((int) val)
                            : String.format("%.2f", val);
                    boolean hov = mouseX >= optX && mouseX < optX + optW
                            && mouseY >= optY && mouseY < optY + ROW_H;
                    ctx.fill(optX, optY, optX + optW, optY + ROW_H, hov ? 0xBB1E1E2E : 0xBB15151E);
                    ctx.drawTextWithShadow(tr, ni.label(), optX + 6, optY + (ROW_H - 8) / 2, 0xFFCCCCDD);
                    int btnW  = 16;
                    int plusX = optX + optW - 6 - btnW;
                    int valW  = tr.getWidth(valStr);
                    int valX  = plusX - 6 - valW;
                    int minuX = valX - 6 - btnW;
                    boolean minuHov = mouseX >= minuX && mouseX < minuX + btnW
                            && mouseY >= optY + 3 && mouseY < optY + ROW_H - 3;
                    ctx.fill(minuX, optY + 3, minuX + btnW, optY + ROW_H - 3,
                            minuHov ? 0xFF8B5CF6 : 0xFF3A3A4A);
                    ctx.drawTextWithShadow(tr, "-", minuX + 5, optY + (ROW_H - 8) / 2, 0xFFFFFFFF);
                    ctx.drawTextWithShadow(tr, valStr, valX, optY + (ROW_H - 8) / 2, 0xFFFFFFFF);
                    boolean plusHov = mouseX >= plusX && mouseX < plusX + btnW
                            && mouseY >= optY + 3 && mouseY < optY + ROW_H - 3;
                    ctx.fill(plusX, optY + 3, plusX + btnW, optY + ROW_H - 3,
                            plusHov ? 0xFF8B5CF6 : 0xFF3A3A4A);
                    ctx.drawTextWithShadow(tr, "+", plusX + 4, optY + (ROW_H - 8) / 2, 0xFFFFFFFF);
                    optY += ROW_H + 2;
                }

                case Option.Label lbl -> {
                    ctx.drawTextWithShadow(tr, lbl.text(), optX + 4, optY + 2, lbl.color());
                    optY += 14;
                }

                case Option.Spacer sp -> optY += sp.pixels();

                case Option.EntryList bl -> {
                    int stateIdx = findEntryListState(bl);
                    if (stateIdx < 0) { optY += 20; break; }
                    EntryListState state = EntryListStates.get(stateIdx);
                    List<String> list = bl.getList().get();

                    ctx.drawTextWithShadow(tr, bl.label(), optX + 4, optY + 2, 0xFF888899);
                    optY += 14;

                    int listH = Math.max(50, list.size() * ROW_H + 4);
                    ctx.fill(optX, optY, optX + optW, optY + listH, 0xBB0C0C14);
                    int rowY = optY + 2;
                    for (int i = 0; i < list.size(); i++) {
                        boolean sel = state.selectedRow == i;
                        boolean hov = mouseX >= optX && mouseX < optX + optW
                                && mouseY >= rowY && mouseY < rowY + ROW_H;
                        ctx.fill(optX, rowY, optX + optW, rowY + ROW_H,
                                sel ? 0xDD2A1A4A : hov ? 0xBB1E1E2E : 0xBB0E0E18);
                        ctx.drawTextWithShadow(tr, (sel ? ">> " : "   ") + list.get(i),
                                optX + 6, rowY + (ROW_H - 8) / 2,
                                sel ? 0xFFFFFFFF : 0xFFAAAAAA);
                        rowY += ROW_H;
                    }
                    if (list.isEmpty())
                        ctx.drawTextWithShadow(tr, "Empty - type below and press Enter",
                                optX + 8, optY + 16, 0xFF444455);
                    optY += listH + 4;

                    // Update TextField position so it renders in the right place
                    int fieldW = optW - 58;
                    state.field.setX(optX);
                    state.field.setY(optY);
                    state.field.setWidth(fieldW);
                    // Cache for click detection
                    state.fieldY = optY;
                    state.addX   = optX + fieldW + 4;
                    state.remX   = state.addX + 28;

                    boolean addHov = mouseX >= state.addX && mouseX < state.addX + 24
                            && mouseY >= optY && mouseY < optY + 16;
                    ctx.fill(state.addX, optY, state.addX + 24, optY + 16,
                            addHov ? 0xFF8B5CF6 : 0xFF5B3CA6);
                    ctx.drawTextWithShadow(tr, "+", state.addX + 8, optY + 4, 0xFFFFFFFF);

                    boolean canRem = state.selectedRow >= 0 && state.selectedRow < list.size();
                    boolean remHov = canRem && mouseX >= state.remX && mouseX < state.remX + 24
                            && mouseY >= optY && mouseY < optY + 16;
                    ctx.fill(state.remX, optY, state.remX + 24, optY + 16,
                            canRem ? (remHov ? 0xFFCF4040 : 0xFF8F2020) : 0xFF333340);
                    ctx.drawTextWithShadow(tr, "x", state.remX + 7, optY + 4,
                            canRem ? 0xFFFFFFFF : 0xFF555555);

                    optY += 22 + SECTION_GAP;
                }
            }
        }

        // Render registered child widgets (TextFieldWidgets) last
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPill(DrawContext ctx, int tx, int ty, boolean on) {
        ctx.fill(tx, ty, tx + 28, ty + 14, on ? 0xFF8B5CF6 : 0xFF3A3A4A);
        int kx = on ? tx + 14 : tx;
        ctx.fill(kx + 2, ty + 2, kx + 12, ty + 12, 0xFFFFFFFF);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        net.minecraft.client.font.TextRenderer tr = textRenderer;

        // Sidebar tab clicks
        if (mx < SIDEBAR_W) {
            int tabY = 32;
            for (int i = 0; i < tabs.size(); i++) {
                if (my >= tabY && my < tabY + ROW_H) {
                    activeTab = i;
                    init(); // rebuilds widgets for new tab only
                    return true;
                }
                tabY += ROW_H + 2;
            }
            return true;
        }

        // Walk options — handle toggle/hotkey BEFORE delegating to super
        // so child widgets don't steal the click
        int optX = SIDEBAR_W + PADDING;
        int optY = 36;
        int optW = width - SIDEBAR_W - PADDING * 2;

        for (Option opt : tabs.get(activeTab).options) {
            switch (opt) {
                case Option.Toggle tog -> {
                    if (my >= optY && my < optY + ROW_H && mx >= optX && mx < optX + optW) {
                        tog.set().accept(!tog.get().get());
                        return true;
                    }
                    optY += ROW_H + 2;
                }
                case Option.Hotkey hk -> {
                    if (my >= optY && my < optY + ROW_H && mx >= optX && mx < optX + optW) {
                        hk.startRebind().run();
                        return true;
                    }
                    optY += ROW_H + 2;
                }
                case Option.NumInput ni -> {
                    float val = ni.get().get();
                    String valStr = (val == (int) val)
                            ? String.valueOf((int) val)
                            : String.format("%.2f", val);
                    int valW = tr.getWidth(valStr);
                    int btnW = 16;
                    int minuX = optX + optW - 30 - valW / 2 - btnW - 6;
                    int plusX = optX + optW - 6 - btnW;

                    if (my >= optY + 3 && my < optY + ROW_H - 3) {
                        if (mx >= minuX && mx < minuX + btnW) {
                            ni.set().accept(Math.max(ni.min(), val - ni.step()));
                            return true;
                        }
                        if (mx >= plusX && mx < plusX + btnW) {
                            ni.set().accept(Math.min(ni.max(), val + ni.step()));
                            return true;
                        }
                    }
                    optY += ROW_H + 2;
                }

                case Option.Label lbl -> optY += 14;
                case Option.Spacer sp  -> optY += sp.pixels();
                case Option.EntryList bl -> {
                    int stateIdx = findEntryListState(bl);
                    if (stateIdx < 0) { optY += 20; break; }
                    EntryListState state = EntryListStates.get(stateIdx);
                    List<String> list = bl.getList().get();

                    optY += 14;
                    int listH = Math.max(50, list.size() * ROW_H + 4);

                    int rowY = optY + 2;
                    for (int i = 0; i < list.size(); i++) {
                        if (my >= rowY && my < rowY + ROW_H && mx >= optX && mx < optX + optW) {
                            state.selectedRow = (state.selectedRow == i) ? -1 : i;
                            return true;
                        }
                        rowY += ROW_H;
                    }
                    optY += listH + 4;

                    if (mx >= state.addX && mx < state.addX + 24
                            && my >= state.fieldY && my < state.fieldY + 16) {
                        addToList(state, bl); return true;
                    }
                    if (mx >= state.remX && mx < state.remX + 24
                            && my >= state.fieldY && my < state.fieldY + 16) {
                        removeSelected(state, bl); return true;
                    }
                    optY += 22 + SECTION_GAP;
                }
            }
        }

        // Let MC route remaining clicks to child widgets (TextField focus etc.)
        return super.mouseClicked(click, doubled);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        if (keyCode == 256) { close(); return true; }

        for (Option opt : tabs.get(activeTab).options) {
            if (opt instanceof Option.EntryList bl) {
                int idx = findEntryListState(bl);
                if (idx < 0) continue;
                EntryListState state = EntryListStates.get(idx);
                if (state.field.isFocused() && (keyCode == 257 || keyCode == 335)) {
                    addToList(state, bl); return true;
                }
                if (!state.field.isFocused() && (keyCode == 261 || keyCode == 259)
                        && state.selectedRow >= 0) {
                    removeSelected(state, bl); return true;
                }
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addToList(EntryListState state, Option.EntryList bl) {
        String text = state.field.getText().trim();
        List<String> list = bl.getList().get();
        if (!text.isEmpty() && !list.contains(text)) {
            list.add(text);
            bl.onChanged().accept(list);
            state.field.setText("");
            state.field.setSuggestion("e.g. minecraft:stone");
        }
    }

    private void removeSelected(EntryListState state, Option.EntryList bl) {
        List<String> list = bl.getList().get();
        if (state.selectedRow >= 0 && state.selectedRow < list.size()) {
            list.remove(state.selectedRow);
            bl.onChanged().accept(list);
            state.selectedRow = Math.min(state.selectedRow, list.size() - 1);
        }
    }

    private int findEntryListState(Option.EntryList bl) {
        for (int i = 0; i < EntryListStates.size(); i++)
            if (EntryListStates.get(i).option == bl) return i;
        return -1;
    }

    // ── Inner ─────────────────────────────────────────────────────────────────

    private static class EntryListState {
        final Option.EntryList option;
        final TextFieldWidget  field;
        int selectedRow = -1;
        int fieldY, addX, remX;

        EntryListState(Option.EntryList option, TextFieldWidget field) {
            this.option = option;
            this.field  = field;
        }
    }
}