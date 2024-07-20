package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.KeyEvent;
import com.kyfexuwu.m3we.editor.component.Component;
import com.kyfexuwu.m3we.editor.component.ComponentFactory;
import com.kyfexuwu.m3we.editor.component.HFillingComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class StrInputComponent extends HFillingComponent {
    public static ComponentFactory get(){//todo
        return new ThisBlueprint();
    }

    private String text="";
    private double lastWidth=0;
    private double textHeight=0;
    private StrInputComponent(Block parent) {
        super(parent, 0,0);
    }

    @Override
    public double width(boolean isolated) { return this.lastWidth+4; }

    @Override
    public double height(boolean isolated) { return textHeight+2; }

    @Override
    public boolean keyTyped(KeyEvent event) {
        if(event.type==KeyEvent.Type.CHAR) this.text+=event.chr;
        else if(event.type==KeyEvent.Type.BACKSPACE) this.text=this.text.substring(0,Math.max(0,this.text.length()-1));

        return true;
    }

    private static final Color bg = new Color(255,255,255);
    @Override
    public void draw(MatrixStack matrices, TextRenderer text, Color color) {
        var height=this.height();
        var width=this.width();
        BlockDrawHelper.vertexes(matrices, bg, c->{
            c.vertex(0,0);
            c.vertex(0,height);
            c.vertex(width,height);
            c.vertex(width,0);
        });

        this.textHeight =text.fontHeight;
        this.lastWidth=text.draw(matrices, '"'+this.text+'"', 2,2, 0xff000000);
    }

    //--

    private static class ThisBlueprint implements ComponentFactory {
        @Override
        public Component create(Block block, int x, int y, ComponentFactory[][] allBlueprints) {
            return new StrInputComponent(block);
        }
    }
}
