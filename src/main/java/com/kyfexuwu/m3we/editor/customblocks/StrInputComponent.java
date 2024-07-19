package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.Component;
import com.kyfexuwu.m3we.editor.component.HFillingComponent;
import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class StrInputComponent extends HFillingComponent {
    public static Blueprint get(String name){
        return new ThisBlueprint(name);
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

    private static class ThisBlueprint extends Blueprint{
        protected ThisBlueprint(String name) {
            super(Type.CUSTOM, name);
        }

        @Override
        public Component create(Block block, int x, int y, Blueprint[][] allBlueprints) {
            return new StrInputComponent(block);
        }
    }
}
