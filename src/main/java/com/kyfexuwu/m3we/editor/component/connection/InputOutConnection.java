package com.kyfexuwu.m3we.editor.component.connection;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.Vec2d;
import com.kyfexuwu.m3we.editor.component.Component;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class InputOutConnection extends Connection{
    public InputOutConnection(Block parent, String name) { super(parent, name); }

    @Override
    public double width(boolean isolated) { return 5; }

    @Override
    public double height(boolean isolated) {
        return Math.max(8, Component.fillRowHeight(this,isolated, 8));
    }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(0,5);
            c.vertex(0,3);
            c.vertex(-4,1);
            c.vertex(-4,7);
            c.vertex(0,5);
            var height=this.height();
            c.vertex(0,height);
            c.vertex(5,height);
            c.vertex(5,0);
            c.vertex(1,0);
            c.vertex(0,0);
        });

    }

    @Override
    public Vec2d connPos() { return super.getConnPos(0,5); }

}
