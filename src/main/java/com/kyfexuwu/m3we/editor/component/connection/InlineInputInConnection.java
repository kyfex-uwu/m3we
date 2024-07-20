package com.kyfexuwu.m3we.editor.component.connection;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.Vec2d;
import com.kyfexuwu.m3we.editor.component.Component;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class InlineInputInConnection extends InputInConnection {
    public InlineInputInConnection(Block parent, String name) { super(parent, name); }

    @Override
    public boolean mouseable(double localX, double localY) {
        return localX<5;
    }

    @Override
    public double width(boolean isolated) {
        if(this.connected==null) return 10;

        double width=0;
        for(var row : this.connected.parent.components)
            width=Math.max(width, Component.rowWidth(row));
        return width+5;
    }

    @Override
    public double height(boolean isolated) {
        var childHeight=0f;
        if(this.connected!=null){
            for(var row : this.connected.parent.components)
                childHeight+=Component.rowHeight(row);
        }

        return Math.max(Math.max(14, Component.fillRowHeight(this,isolated, 14)), childHeight);
    }

    @Override
    public Vec2d connPos() {
        return new Vec2d(5,10);
    }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(0,0);
            c.vertex(1,6);
            c.vertex(5,8);
            c.vertex(5,0);
        });
        BlockDrawHelper.vertexes(matrices, color, c->{
            var height = this.height();
            c.vertex(0,height);
            c.vertex(5,height);
            c.vertex(5,10);
            c.vertex(1,12);
            c.vertex(1,6);
            c.vertex(0,0);
        });
    }
}
