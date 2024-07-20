package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;

public abstract class FillingComponent extends Component{
    private final double minWidth;
    private final double minHeight;
    public FillingComponent(Block parent, double minWidth, double minHeight) {
        super(parent);
        this.minWidth=minWidth;
        this.minHeight=minHeight;
    }
    public FillingComponent(Block parent) {
        this(parent, 0, 0);
    }

    @Override
    public double width(boolean isolated){
        if(isolated) return this.minWidth;

        var width = this.minWidth;
        double restOfRow=0;
        for(var row : this.parent.components)
            if(Component.arrContains(this, row)) restOfRow = Component.rowWidth(row)-this.width(true);
        for(var row : this.parent.components){
            if(Component.arrContains(this, row)) continue;
            width=Math.max(width, Component.rowWidth(row)-restOfRow);
        }
        return width;
    }
    @Override
    public double height(boolean isolated){
        if(isolated) return this.minHeight;

        for(var row : this.parent.components)
            if(Component.arrContains(this, row)) return Component.rowHeight(row);
        return this.minHeight;
    }
}
