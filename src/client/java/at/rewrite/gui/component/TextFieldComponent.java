package at.rewrite.gui.component;

import net.minecraft.client.gui.DrawContext;

public class TextFieldComponent {

    private String text="";

    public void set(String t){
        text=t;
    }

    public String get(){
        return text;
    }

    public void render(DrawContext ctx,int x,int y,int width){

        ctx.fill(x,y,x+width,y+14,0xFF2A2A35);

    }

}