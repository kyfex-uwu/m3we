package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.component.ComponentFactory;
import com.kyfexuwu.m3we.editor.component.TextComponent;
import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;
import com.kyfexuwu.m3we.editor.component.connection.InlineInputInConnection;
import com.kyfexuwu.m3we.editor.component.connection.InputInConnection;
import com.kyfexuwu.m3we.editor.component.connection.SeqInConnection;
import com.kyfexuwu.m3we.editor.component.connection.SeqOutConnection;

import java.util.function.Function;

public class IfBlock extends Block {
    private static final ComponentFactory[][] components;
    static{

        components = new ComponentFactory[][]{
                new ComponentFactory[]{
                        (block, x,y,a) -> new Blueprint.CornerComponent(block, Blueprint.Corner.TL),
                        (block,x,y,a)->new SeqInConnection(block, "prev"),
                        (block,x,y,a)->new Blueprint.HWallComponent(block, Blueprint.HSide.TOP),
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.TR)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        (block,x,y,a)->new TextComponent(block,"if"),
                        (block,x,y,a)->new InlineInputInConnection(block,"condition"),
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.RIGHT)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        (block,x,y,a)->new VFillingSpacerComponent(block, 5),
                        (block,x,y,a)->new SeqOutConnection(block, "child"),
                        (block,x,y,a)->new Blueprint.InsideComponent(block),
                        (block,x,y,a)->new Blueprint.CornerComponent(block,Blueprint.Corner.BR)},
                new ComponentFactory[]{
                        (block,x,y,a)->new ContainingComponent(block)},

                //todo:replace insides with shorter (h) insides
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        (block,x,y,a)->new Blueprint.HWallComponent(block, Blueprint.HSide.TOP),
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.TR)},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.VWallComponent(block, Blueprint.VSide.LEFT),
                        (block,x,y,a)->new Blueprint.InsideComponent(block),
                        (block,x,y,a)->new InputInConnection(block, "connect")},
                new ComponentFactory[]{
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.BL),
                        (block,x,y,a)->new SeqOutConnection(block, "next"),
                        (block,x,y,a)->new Blueprint.HWallComponent(block, Blueprint.HSide.BOTTOM),
                        (block,x,y,a)->new Blueprint.CornerComponent(block, Blueprint.Corner.BR)},
        };
    }
    private static final Function<Block, String> exportFunc = block -> {
        return "<if statement>";
    };
    public IfBlock() {
        super(Blocks.Colors.FLOW_CONTROL, components, exportFunc);
    }
}
