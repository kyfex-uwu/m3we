package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;

public abstract class NonResizableComponent extends Component{
    private final float _width;
    private final float _height;
    public NonResizableComponent(Block parent, float width, float height) {
        super(parent);
        this._width=width;
        this._height=height;
    }

    @Override
    public float width(boolean isolated) { return this._width; }

    @Override
    public float height(boolean isolated) { return this._height; }
}
