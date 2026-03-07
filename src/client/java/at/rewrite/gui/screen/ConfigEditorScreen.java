package at.rewrite.gui.screen;

import at.rewrite.gui.core.UIContainer;
import at.rewrite.gui.panel.ConfigListPanel;
import at.rewrite.gui.panel.SettingsPanel;
import at.rewrite.gui.panel.SidebarPanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class ConfigEditorScreen extends Screen {

    private final UIContainer root = new UIContainer();
    private SidebarPanel sidebar;
    private ConfigListPanel listPanel;
    private SettingsPanel settingsPanel;

    public ConfigEditorScreen() {
        super(Text.literal("Rewrite Config"));
    }

    @Override
    protected void init(){
        String[] categories = {"Targets","Mining","Tracking"};

        Runnable[] callbacks = new Runnable[categories.length];
        for(int i=0;i<categories.length;i++){
            final int idx=i;
            callbacks[i] = () -> {
                if(idx==0) listPanel.setEntries(List.of("Goblin","Worm","Bal"));
                if(idx==1) listPanel.setEntries(List.of("AutoMine","Pathfinder"));
                if(idx==2) listPanel.setEntries(List.of("Option1","Option2"));
            };
        }

        sidebar = new SidebarPanel(categories, callbacks);
        sidebar.setBounds(0,0,140,height);

        listPanel = new ConfigListPanel();
        listPanel.setBounds(140,0,200,height);

        settingsPanel = new SettingsPanel();
        settingsPanel.setBounds(340,0,width-340,height);

        root.add(sidebar);
        root.add(listPanel);
        root.add(settingsPanel);
    }

    @Override
    public void render(DrawContext ctx,int mouseX,int mouseY,float delta){
        ctx.fill(0,0,width,height,0x88000000);
        root.render(ctx,mouseX,mouseY);
        super.render(ctx,mouseX,mouseY,delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled){
        return root.mouseClicked(click.x(),click.y());
    }

    @Override
    public void close(){
        client.setScreen(null);
    }
}