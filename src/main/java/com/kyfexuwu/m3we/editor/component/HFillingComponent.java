package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;

public abstract class HFillingComponent extends Component{
    private final double _height;
    private final double minWidth;
    public HFillingComponent(Block parent, double height, double minWidth) {
        super(parent);
        this.minWidth=minWidth;
        this._height=height;
    }
    public HFillingComponent(Block parent, double height) {
        this(parent, height, 0);
    }

    @Override
    public double width(boolean isolated){ return Math.max(this.minWidth,
            Component.fillRowWidth(this, isolated, this.minWidth)); }
    @Override
    public double height(boolean isolated){ return this._height; }
}
