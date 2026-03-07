package at.rewrite.gui.panel;

import at.rewrite.gui.component.CategoryButton;
import at.rewrite.gui.core.UIComponent;

public class SidebarPanel extends UIComponent {

    private final CategoryButton[] buttons;
    private int selectedIndex = 0;

    public SidebarPanel(String[] names, Runnable[] callbacks){
        buttons = new CategoryButton[names.length];
        int offsetY = 10;
        for(int i=0;i<names.length;i++){
            final int idx = i;
            CategoryButton b = new CategoryButton(names[i], () -> {
                selectedIndex = idx;
                if(callbacks[idx] != null) callbacks[idx].run();
                updateSelection();
            });
            b.setBounds(0, offsetY, 140, 20);
            buttons[i] = b;
            add(b);
            offsetY += 22;
        }
        updateSelection();
    }

    private void updateSelection(){
        for(int i=0;i<buttons.length;i++){
            buttons[i].setSelected(i==selectedIndex);
        }
    }
}