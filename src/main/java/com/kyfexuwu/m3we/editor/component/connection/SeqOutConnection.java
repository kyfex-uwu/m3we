package com.kyfexuwu.m3we.editor.component.connection;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec2f;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;

public class SeqOutConnection extends Connection{
    public SeqOutConnection(Block parent, String name) { super(parent, name); }

    @Override
    public float width(boolean isolated) { return 8; }

    @Override
    public float height(boolean isolated) { return 5; }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(0,0);
            c.vertex(0,5);
            c.vertex(1,5);
            c.vertex(1,0);
        });
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(7,0);
            c.vertex(7,5);
            c.vertex(8,5);
            c.vertex(8,0);
        });

        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(4,0);
            c.vertex(0,0);
            c.vertex(0,5);

            c.vertex(1,7);
            c.vertex(4,8);
            c.vertex(7,7);

            c.vertex(8,5);
            c.vertex(8,0);
        });
    }

    @Override
    public Vec2f connPos() { return super.getConnPos(4,5); }

}
