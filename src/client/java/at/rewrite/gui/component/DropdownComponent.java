package at.rewrite.gui.component;

import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class DropdownComponent {

    private List<String> options;
    private int selected;

    public DropdownComponent(List<String> options){
        this.options=options;
    }

    public String get(){
        return options.get(selected);
    }

    public void render(DrawContext ctx,int x,int y,int width){

        ctx.fill(x,y,x+width,y+14,0xFF2A2A35);

    }

}