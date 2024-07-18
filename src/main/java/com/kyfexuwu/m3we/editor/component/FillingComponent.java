package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;

public abstract class FillingComponent extends Component{
    private final float minWidth;
    private final float minHeight;
    public FillingComponent(Block parent, float minWidth, float minHeight) {
        super(parent);
        this.minWidth=minWidth;
        this.minHeight=minHeight;
    }
    public FillingComponent(Block parent) {
        this(parent, 0, 0);
    }

    @Override
    public float width(boolean isolated){
        if(isolated) return this.minWidth;

        var width = this.minWidth;
        float restOfRow=0;
        for(var row : this.parent.components)
            if(Component.arrContains(this, row)) restOfRow = Component.rowWidth(row)-this.width();
        for(var row : this.parent.components){
            if(Component.arrContains(this, row)) continue;
            width=Math.max(width, Component.rowWidth(row)-restOfRow);
        }
        return width;
    }
    @Override
    public float height(boolean isolated){
        if(isolated) return 0;

        for(var row : this.parent.components)
            if(Component.arrContains(this, row)) return Component.rowHeight(row);
        return 0;
    }
}
