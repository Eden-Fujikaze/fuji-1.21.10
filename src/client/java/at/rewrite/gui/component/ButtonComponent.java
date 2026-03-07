package at.rewrite.gui.component;

import at.rewrite.gui.core.UIComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ButtonComponent extends UIComponent {

    private final String label;
    private final Runnable action;

    public ButtonComponent(String label,Runnable action){
        this.label=label;
        this.action=action;
    }

    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY){
        ctx.fill(x,y,x+width,y+16,0xFF8B5CF6);
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,label,x+6,y+4,0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY){
        if(mouseX>=x && mouseX<=x+width && mouseY>=y && mouseY<=y+16){
            if(action!=null) action.run();
            return true;
        }
        return false;
    }
}