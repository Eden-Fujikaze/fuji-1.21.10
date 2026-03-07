package at.rewrite.gui.panel;

import at.rewrite.gui.core.UIComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ConfigListPanel extends UIComponent {

    private final List<String> entries = new ArrayList<>();

    public void setEntries(List<String> list){
        entries.clear();
        entries.addAll(list);
    }

    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY){
        ctx.fill(x,y,x+width,y+height,0xFF1E1E28);
        int offset=10;
        for(String e:entries){
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,e,x+10,y+offset,0xFFFFFFFF);
            offset+=16;
        }
        super.render(ctx,mouseX,mouseY);
    }
}