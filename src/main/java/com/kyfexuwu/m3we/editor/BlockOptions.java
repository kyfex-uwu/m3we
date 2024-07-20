package com.kyfexuwu.m3we.editor;


import com.kyfexuwu.m3we.editor.component.ComponentFactory;
import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class BlockOptions implements BlockFactory{

    public enum Type{
        SEQ,
        SEQ_IN,
        SEQ_OUT,
        NONE
    }
    private Color color = new Color(200,200,200);
    private static Function<Block, String> defaultFunc = b->"";
    private Function<Block, String> exportFunc=defaultFunc;
    private ArrayList<ArrayList<ComponentFactory>> components = new ArrayList<>();
    private final Type type;
    public BlockOptions(Type type){
        this.type=type;
    }
    public BlockOptions color(Color color){ this.color=color; return this; }
    public Color color(){ return this.color; }
    public BlockOptions export(Function<Block, String> exportFunc){ this.exportFunc=exportFunc; return this; }
    public Function<Block, String> export(){ return this.exportFunc; }
    public BlockOptions components(ArrayList<ArrayList<ComponentFactory>> components){
        this.components = components;
        return this;
    }
    public BlockOptions insertRow(int index, ArrayList<ComponentFactory> row){
        this.components.add(index,row);
        return this;
    }
    public BlockOptions insertRow(int index, ComponentFactory... row){
        return this.insertRow(index,BlockOptions.fromArr(row));
    }
    public BlockOptions replaceRow(int index, ArrayList<ComponentFactory> row){
        this.components.set(index,row);
        return this;
    }
    public BlockOptions replaceRow(int index, ComponentFactory... row){
        return this.replaceRow(index,BlockOptions.fromArr(row));
    }
    public BlockOptions appendRow(ArrayList<ComponentFactory> row){
        this.components.add(row);
        return this;
    }
    public BlockOptions appendRow(ComponentFactory... row){
        return this.appendRow(BlockOptions.fromArr(row));
    }

    public static ArrayList<ComponentFactory> fromArr(ComponentFactory[] arr){
        return new ArrayList<>(List.of(arr));
    }
    private boolean precreated=false;
    private ComponentFactory[][] blueprintArr;
    public BlockOptions preCreate(){
        if(this.precreated) return this;
        this.precreated=true;

        //top
        var l1=fromArr(new ComponentFactory[]{ Blueprint.SOLID, Blueprint.SOLID,Blueprint.SOLID });
        if(this.type==Type.SEQ_IN||this.type==Type.SEQ)
            l1.add(1,Blueprint.seq("prev"));
        this.insertRow(0, l1);

        //bottom
        var l2=fromArr(new ComponentFactory[]{ Blueprint.SOLID,Blueprint.SOLID,Blueprint.SOLID });
        if(this.type==Type.SEQ_OUT||this.type==Type.SEQ)
            l2.add(1,Blueprint.seq("next"));
        this.appendRow(l2);

        //inner
        for(var y=0;y<this.components.size();y++){
            var row = this.components.get(y);
            //if there is not input in edges, fill with wall
            if(y!=0&&y!=this.components.size()-1) {
                var first = row.get(0);
                var last=row.get(row.size()-1);
                var firstIsInput=first instanceof Blueprint.InputMarker;
                if (!firstIsInput&&!first.equals(Blueprint.SOLID)) row.add(0, Blueprint.SOLID);
                if ((!(last instanceof Blueprint.InputMarker)&&!last.equals(Blueprint.SOLID))||
                        (firstIsInput&&row.size()==1)) row.add(Blueprint.SOLID);
            }
        }

        //array
        this.blueprintArr = new ComponentFactory[this.components.size()][];
        for(var y=0;y<this.components.size();y++){
            var row = this.components.get(y);

            this.blueprintArr[y]=new ComponentFactory[row.size()];
            for(var x=0;x<row.size();x++)
                this.blueprintArr[y][x]=row.get(x);
        }

        return this;
    }
    public ComponentFactory[][] blueprintArr() {
        this.preCreate();
        return this.blueprintArr;
    }
    public Block create(){
        this.preCreate();
        return new Block(this.color,this.blueprintArr(),this.exportFunc);
    }
}
