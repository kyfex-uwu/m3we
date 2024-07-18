package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;

public abstract class HFillingComponent extends Component{
    private final float _height;
    private final float minWidth;
    public HFillingComponent(Block parent, float height, float minWidth) {
        super(parent);
        this.minWidth=minWidth;
        this._height=height;
    }
    public HFillingComponent(Block parent, float height) {
        this(parent, height, 0);
    }

    @Override
    public float width(boolean isolated){ return Math.max(this.minWidth,
            Component.fillRowWidth(this, isolated, this.minWidth)); }
    @Override
    public float height(boolean isolated){ return this._height; }
}
