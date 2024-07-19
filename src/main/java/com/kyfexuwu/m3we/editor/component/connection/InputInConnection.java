package com.kyfexuwu.m3we.editor.component.connection;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.Vec2d;
import com.kyfexuwu.m3we.editor.component.Component;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class InputInConnection extends Connection{
    public InputInConnection(Block parent, String name) { super(parent, name); }

    @Override
    public double width(boolean isolated) { return 5; }

    @Override
    public double height(boolean isolated) {
        return Math.max(8, Component.fillRowHeight(this,isolated, 8));
    }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(0,0);
            c.vertex(1,1);
            c.vertex(5,3);
            c.vertex(5,0);
        });
        BlockDrawHelper.vertexes(matrices, color, c->{
            var height = this.height();
            c.vertex(0,height);
            c.vertex(5,height);
            c.vertex(5,5);
            c.vertex(1,7);
            c.vertex(1,1);
            c.vertex(0,0);
        });
    }

    @Override
    public Vec2d connPos() { return super.getConnPos(5,5); }

}
