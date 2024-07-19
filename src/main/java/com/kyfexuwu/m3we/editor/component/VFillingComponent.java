package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;

public abstract class VFillingComponent extends Component{
    private final double _width;
    private final double minHeight;
    public VFillingComponent(Block parent, double width, double minHeight) {
        super(parent);
        this._width=width;
        this.minHeight=minHeight;
    }
    public VFillingComponent(Block parent, double width) {
        this(parent, width, 0);
    }

    @Override
    public double width(boolean isolated){ return this._width; }
    @Override
    public double height(boolean isolated){ return Math.max(this.minHeight,
            Component.fillRowHeight(this, isolated, this.minHeight)); }
}
