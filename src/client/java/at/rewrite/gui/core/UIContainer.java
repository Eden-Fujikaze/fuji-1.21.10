package at.rewrite.gui.core;

import net.minecraft.client.gui.DrawContext;

public class UIContainer extends UIComponent {
    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY){
        for(UIComponent c:children) c.render(ctx,mouseX,mouseY);
    }
}