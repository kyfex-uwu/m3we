package com.kyfexuwu.m3we.editor.component.blueprint;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.component.Component;
import com.kyfexuwu.m3we.editor.component.ComponentFactory;
import com.kyfexuwu.m3we.editor.component.TextComponent;

public class TextBlueprint implements ComponentFactory {
    private final String text;
    public TextBlueprint(String text){
        this.text=text;
    }

    public Component create(Block block, int x, int y, ComponentFactory[][] allBlueprints) {
        return new TextComponent(block, this.text);
    }
}
