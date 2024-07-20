package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;

public interface ComponentFactory {
    Component create(Block block, int x, int y, ComponentFactory[][] allBlueprints);
}
