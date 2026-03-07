package at.rewrite.gui.component;

import net.minecraft.client.gui.DrawContext;

public class SliderComponent {

    private float min;
    private float max;
    private float value;

    public SliderComponent(float min,float max,float value){
        this.min=min;
        this.max=max;
        this.value=value;
    }

    public void render(DrawContext ctx,int x,int y,int width){

        ctx.fill(x,y,x+width,y+4,0xFF3A3A4A);

        float percent=(value-min)/(max-min);

        int pos=(int)(width*percent);

        ctx.fill(x,y,x+pos,y+4,0xFF8B5CF6);

    }

}