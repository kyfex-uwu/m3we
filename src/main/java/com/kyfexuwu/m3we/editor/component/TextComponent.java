package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;

public class TextComponent extends FillingComponent{
    private final Text text;
    private float lastWidth=0;
    private float textHeight =0;
    public TextComponent(Block parent, String text) {
        super(parent, 0, 12);//todo: replace with min text height
        this.text=Text.literal(text);
    }

    @Override
    public float width(boolean isolated) {
        return this.lastWidth+4;
    }
    @Override
    public float height(boolean isolated) {
        return Math.max(super.height(isolated),this.textHeight);
    }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        var height=this.height();
        BlockDrawHelper.vertexes(matrices, color, c->{
            var width=this.width();
            c.vertex(0,0);
            c.vertex(0,height);
            c.vertex(width,height);
            c.vertex(width,0);
        });

        var isWhite = Math.sqrt(color.r()*0.299 + color.g()*0.587 + color.b()*0.114)<186;
        this.textHeight =text.fontHeight;

        this.lastWidth=text.draw(matrices, this.text, 2,(height-this.textHeight)/2, isWhite?0xffffffff:0xff000000);
    }
}
