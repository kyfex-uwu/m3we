package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.VFillingComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class VFillingSpacerComponent extends VFillingComponent {
    public VFillingSpacerComponent(Block parent, double width) {
        super(parent, width);
    }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        BlockDrawHelper.vertexes(matrices, color, c->{
            var height=this.height();
            var width=this.width();
            c.vertex(0,0);
            c.vertex(0,height);
            c.vertex(width,height);
            c.vertex(width,0);
        });
    }
}
