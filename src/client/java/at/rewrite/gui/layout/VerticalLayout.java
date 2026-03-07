package at.rewrite.gui.layout;

import at.rewrite.gui.core.UIComponent;

public class VerticalLayout {
    public static void apply(UIComponent parent,int spacing){
        int offset = parent.y;
        for(UIComponent c: parent.children){
            c.setBounds(parent.x, offset, parent.width, 20);
            offset += 20 + spacing;
        }
    }
}