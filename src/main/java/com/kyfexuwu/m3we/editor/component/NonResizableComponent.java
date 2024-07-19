package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;

public abstract class NonResizableComponent extends Component{
    private final double _width;
    private final double _height;
    public NonResizableComponent(Block parent, double width, double height) {
        super(parent);
        this._width=width;
        this._height=height;
    }

    @Override
    public double width(boolean isolated) { return this._width; }

    @Override
    public double height(boolean isolated) { return this._height; }
}
