package at.fuji.ui;

import net.minecraft.client.gui.DrawContext;

public interface Sidebar {
    String getLabel();

    int getAccentColor();

    void init(FujiScreen parent, int panelX, int panelY, int panelW, int panelH);

    void render(DrawContext gfx, int mouseX, int mouseY, float delta);

    void clearWidgets();

    default boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }
}