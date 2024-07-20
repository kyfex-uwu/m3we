package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.ComponentFactory;
import com.kyfexuwu.m3we.editor.component.NonResizableComponent;
import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;
import com.kyfexuwu.m3we.editor.component.blueprint.TextBlueprint;
import com.kyfexuwu.m3we.editor.component.connection.SeqOutConnection;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Function;

public class FunctionBlock extends Block {
    private static ComponentFactory[][] components;
    static{
        components = new ComponentFactory[][]{
                new ComponentFactory[]{
                        (block, x,y,a) -> new FunctionHeadComponent(block),
                        (block,x,y,a)->new Blueprint.HWallComponent(block, Blueprint.HSide.TOP),
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.TR)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        new TextBlueprint("function"),
                        StrInputComponent.get(),
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.RIGHT)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        (block,x,y,a)->new VFillingSpacerComponent(block, 5),
                        (block,x,y,a)->new SeqOutConnection(block, "child"),
                        (block,x,y,a)->new Blueprint.InsideComponent(block),
                        (block,x,y,a)->new Blueprint.CornerComponent(block,Blueprint.Corner.BR)},
                new ComponentFactory[]{
                        (block,x,y,a)->new ContainingComponent(block)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.TR)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.BL),
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.BR)}
        };
    }
    private static Function<Block, String> export = block -> {
        return "<function>";
    };

    private static class FunctionHeadComponent extends NonResizableComponent{
        public FunctionHeadComponent(Block parent) {
            super(parent, 40, 5);
        }

        @Override
        public void draw(MatrixStack matrices, TextRenderer text, Color color) {
            BlockDrawHelper.vertexes(matrices, color, c->{
                c.vertex(0,2);
                c.vertex(0,5);
                c.vertex(40,5);
                c.vertex(40,0);

                c.vertex(34,-3);
                c.vertex(20,-5);
                c.vertex(6,-3);
                c.vertex(1,0);
            });
        }
    }

    //--

    public FunctionBlock() {
        super(Blocks.Colors.FLOW_CONTROL, components, export);
    }
}
