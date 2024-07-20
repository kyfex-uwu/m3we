package com.kyfexuwu.m3we.editor.customblocks;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.ComponentFactory;

import java.util.function.Function;

public class StrInputBlock extends Block {
    private final StrInputComponent strInputComponent;
    public StrInputBlock(Color color, ComponentFactory[][] components, Function<Block, String> export) {
        super(color, components, export);

        StrInputComponent input=null;
        for(var row : this.components){
            for(var c : row){
                if(c instanceof StrInputComponent casted){
                    input=casted;
                    break;
                }
            }
            if(input!=null) break;
        }
        this.strInputComponent=input;
        if(input==null) throw new IllegalStateException("No StrInputComponent in StrInputBlock bruh");
    }
}
