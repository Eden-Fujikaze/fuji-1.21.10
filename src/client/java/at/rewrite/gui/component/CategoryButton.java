package at.rewrite.gui.component;

import at.rewrite.gui.core.UIComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CategoryButton extends UIComponent {
    private final String label;
    private final Runnable onClick;
    private boolean selected = false;

    public CategoryButton(String label, Runnable onClick){
        this.label = label;
        this.onClick = onClick;
    }

    public void setSelected(boolean sel){ selected = sel; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY){
        int bg = selected ? 0xFF8B5CF6 : 0xFF15151D;
        ctx.fill(x,y,x+width,y+height,bg);
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,label,x+6,y+4,0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY){
        if(mouseX >= x && mouseX <= x+width && mouseY >= y && mouseY <= y+height){
            if(onClick != null) onClick.run();
            return true;
        }
        return false;
    }
}