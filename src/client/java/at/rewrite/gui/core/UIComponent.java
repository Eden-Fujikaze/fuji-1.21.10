package at.rewrite.gui.core;

import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public class UIComponent {

    public int x;
    public int y;
    public int width;
    protected int height;

    public List<UIComponent> children = new ArrayList<>();

    public void setBounds(int x,int y,int w,int h){
        this.x=x;
        this.y=y;
        this.width=w;
        this.height=h;
    }

    public void add(UIComponent c){
        children.add(c);
    }

    public void render(DrawContext ctx,int mouseX,int mouseY){

        for(UIComponent c:children){
            c.render(ctx,mouseX,mouseY);
        }

    }

    public boolean mouseClicked(double mouseX,double mouseY){

        for(UIComponent c:children){
            if(c.mouseClicked(mouseX,mouseY)) return true;
        }

        return false;
    }

}
