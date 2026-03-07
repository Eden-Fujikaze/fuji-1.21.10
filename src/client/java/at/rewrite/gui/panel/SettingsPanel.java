package at.rewrite.gui.panel;

import at.rewrite.gui.core.UIComponent;
import net.minecraft.client.gui.DrawContext;

public class SettingsPanel extends UIComponent {
    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY){
        ctx.fill(x,y,x+width,y+height,0xFF252530);
        super.render(ctx,mouseX,mouseY);
    }
}