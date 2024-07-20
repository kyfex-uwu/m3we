package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockFactory;
import com.kyfexuwu.m3we.editor.BlockOptions;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;
import com.kyfexuwu.m3we.editor.component.blueprint.TextBlueprint;

public class Blocks {
    public static class Colors{
        public static final Color FLOW_CONTROL = new Color(242, 185, 41);
    }

    public static final BlockFactory FUNCTION;
    public static final BlockFactory IF;

    public static final BlockFactory PRINT;

    public static final BlockFactory PLUS;

    public static final BlockFactory STRING;

    //--

    static{
        FUNCTION = FunctionBlock::new;
        IF = IfBlock::new;

        PRINT = new BlockOptions(BlockOptions.Type.SEQ)
                .appendRow(new TextBlueprint("print"), Blueprint.input("toPrint"))
                .color(new Color(181, 11, 178)).export(b->{
                    return "print("+Block.getExport(b,"toPrint")+")";
                });
        PLUS = new BlockOptions(BlockOptions.Type.NONE)
                .appendRow(Blueprint.input("out"), Blueprint.input("left"),
                        new TextBlueprint("+"), Blueprint.input("right"))
                .color(new Color(38, 90, 181)).export(b->{
                    return Block.getExport(b,"left")+"+"+Block.getExport(b,"right");
                });

        var strInputBlockOptions=new BlockOptions(BlockOptions.Type.NONE)
                .appendRow(Blueprint.input("left"), StrInputComponent.get())
                .color(new Color(242, 188, 61)).export(b->{
                    return Block.getExport(b,"value");
                }).preCreate();
        STRING = () -> new StrInputBlock(
                strInputBlockOptions.color(),
                strInputBlockOptions.blueprintArr(),
                strInputBlockOptions.export());
    }
}
