package at.fuji.ui;

import net.minecraft.client.gui.GuiGraphics;

public interface Sidebar {
    String getLabel();

    int getAccentColor();

    void init(FujiScreen parent, int panelX, int panelY, int panelW, int panelH);

    void render(GuiGraphics gfx, int mouseX, int mouseY, float delta);

    void clearWidgets();
}