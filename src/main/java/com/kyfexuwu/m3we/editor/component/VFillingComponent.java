package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;

public abstract class VFillingComponent extends Component{
    private final float _width;
    private final float minHeight;
    public VFillingComponent(Block parent, float width, float minHeight) {
        super(parent);
        this._width=width;
        this.minHeight=minHeight;
    }
    public VFillingComponent(Block parent, float width) {
        this(parent, width, 0);
    }

    @Override
    public float width(boolean isolated){ return this._width; }
    @Override
    public float height(boolean isolated){ return Math.max(this.minHeight,
            Component.fillRowHeight(this, isolated, this.minHeight)); }
}
