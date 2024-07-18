package com.kyfexuwu.m3we.editor.component.blueprint;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.component.Component;
import com.kyfexuwu.m3we.editor.component.TextComponent;

public class TextBlueprint extends Blueprint{
    private final String text;
    public TextBlueprint(String text){
        super(Type.CUSTOM);
        this.text=text;
    }

    @Override
    public Component create(Block block, int x, int y, Blueprint[][] allBlueprints) {
        return new TextComponent(block, this.text);
    }
}
