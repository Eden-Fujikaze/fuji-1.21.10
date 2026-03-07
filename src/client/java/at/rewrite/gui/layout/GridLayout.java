package at.rewrite.gui.layout;

import java.util.ArrayList;
import java.util.List;

public class GridLayout {

    private int columns;

    private final List<Object> components = new ArrayList<>();

    public GridLayout(int columns){
        this.columns=columns;
    }

    public void add(Object c){
        components.add(c);
    }

}