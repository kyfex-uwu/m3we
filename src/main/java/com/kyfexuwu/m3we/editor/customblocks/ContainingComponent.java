package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.Component;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

public class ContainingComponent extends Component {
    public ContainingComponent(Block parent) {
        super(parent);
    }

    @Override
    public double width(boolean isolated) {
        return 5;
    }

    @Override
    public double height(boolean isolated) {
        var child = this.parent.connections.get("child").getConnection();
        var height=0.0;

        while(true){
            if(child==null) break;

            height+=child.parent.boundingBox().height();

            var next = child.parent.connections.get("next");
            if(next==null||!next.isConnected()) break;
            child = next.getConnection();
        }

        return Math.max(10,height+5);
    }

    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(0,0);
            c.vertex(0,2.5);
            c.vertex(2.5,5);
            c.vertex(5,2.5);
            c.vertex(5,0);
        });
        var height=this.height();
        BlockDrawHelper.vertexes(matrices, color, c->{
            c.vertex(0,height-2.5);
            c.vertex(0,height);
            c.vertex(5,height);
            c.vertex(5,height-2.5);
            c.vertex(2.5,height-5);
        });

        for(int i=8;i<height-7;i+=5)
            DrawableHelper.fill(matrices, 2,i,3,i+1, color.toInt());
    }
}
