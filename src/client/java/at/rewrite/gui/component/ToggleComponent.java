package at.rewrite.gui.component;

import at.rewrite.gui.core.UIComponent;
import net.minecraft.client.gui.DrawContext;

public class ToggleComponent extends UIComponent {

    private boolean value;

    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY){
        int color = value ? 0xFF8B5CF6 : 0xFF3A3A4A;
        ctx.fill(x,y,x+20,y+12,color);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY){
        if(mouseX>=x && mouseX<=x+20 && mouseY>=y && mouseY<=y+12){
            value=!value;
            return true;
        }
        return false;
    }
}