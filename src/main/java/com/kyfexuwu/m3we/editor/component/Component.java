package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec2f;

public abstract class Component{
    public final Block parent;
    public Component(Block parent){
        this.parent = parent;
    }
    public float x(){
        for(var row : this.parent.components){
            var x=0;
            for(var c : row){
                if(c==this) return x;

                x+=c.width();
            }
        }
        return 0;
    }
    public float y(){
        float y=0;
        for(var row : this.parent.components){
            for(var c : row) if(c==this) return y;

            if(row.length>0){
                float height=0;
                for(var c : row) height=Math.max(c.height(), height);

                y+=height;
            }
        }
        return 0;
    }
    public Vec2f globalPos(){
        var pos = this.parent.getPos();
        return new Vec2f(this.x()+pos.x, this.y()+pos.y);
    }
    public abstract float width(boolean isolated);
    public float width(){ return this.width(false); }
    public abstract float height(boolean isolated);
    public float height(){ return this.height(false); }
    public boolean mouseable(float localX, float localY){ return true; }

    public abstract void draw(MatrixStack matrices, TextRenderer text, Color color);

    //--

    public static Component[] getRow(Component component){
        for(var row : component.parent.components)
            for(var c : row) if(c==component) return row;
        return null;
    }
    public static float rowHeight(Component[] row){
        if(row.length==0) return 0;

        float height=0;
        for(var c : row) height=Math.max(c.height(true), height);

        return height;
    }
    public static float rowWidth(Component[] row){
        if(row.length==0) return 0;

        float width=0;
        for(var c : row) width+=c.width(true);

        return width;
    }
    public static float fillRowWidth(Component component, boolean isolated, float dfault){
        if(isolated) return dfault;

        float w=0;
        for(var row : component.parent.components) w=Math.max(w, Component.rowWidth(row));
        for(var c : Component.getRow(component)) if(c!=component) w-=c.width();

        return w;
    }
    public static float fillRowHeight(Component component, boolean isolated, float dfault){
        return isolated?dfault:Component.rowHeight(Component.getRow(component));
    }

    public static <T> boolean arrContains(T val, T[] arr){
        for(var v : arr) if(val.equals(v)) return true;
        return false;
    }
}