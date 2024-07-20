package com.kyfexuwu.m3we.editor.component;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.KeyEvent;
import com.kyfexuwu.m3we.editor.Vec2d;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public abstract class Component{
    public final Block parent;
    public Component(Block parent){
        this.parent = parent;
    }
    public double x(){
        for(var row : this.parent.components){
            var x=0;
            for(var c : row){
                if(c==this) return x;

                x+=c.width();
            }
        }
        return 0;
    }
    public double y(){
        double y=0;
        for(var row : this.parent.components){
            for(var c : row) if(c==this) return y;

            if(row.length>0){
                double height=0;
                for(var c : row) height=Math.max(c.height(), height);

                y+=height;
            }
        }
        return 0;
    }
    public Vec2d globalPos(){
        var pos = this.parent.getPos();
        return new Vec2d(this.x()+pos.x, this.y()+pos.y);
    }
    public abstract double width(boolean isolated);
    public double width(){ return this.width(false); }
    public abstract double height(boolean isolated);
    public double height(){ return this.height(false); }
    public boolean mouseable(double localX, double localY){ return true; }
    public boolean click(double x, double y){ return false; }
    public boolean keyTyped(KeyEvent event){ return false; }

    public abstract void draw(MatrixStack matrices, TextRenderer text, Color color);

    //--

    public static Component[] getRow(Component component){
        for(var row : component.parent.components)
            for(var c : row) if(c==component) return row;
        return null;
    }
    public static double rowHeight(Component[] row){
        if(row.length==0) return 0;

        double height=0;
        for(var c : row) height=Math.max(c.height(true), height);

        return height;
    }
    public static double rowWidth(Component[] row){
        if(row.length==0) return 0;

        double width=0;
        for(var c : row) width+=c.width(true);

        return width;
    }
    public static double fillRowWidth(Component component, boolean isolated, double dfault){
        if(isolated) return dfault;

        double w=0;
        for(var row : component.parent.components) w=Math.max(w, Component.rowWidth(row));
        for(var c : Component.getRow(component)) if(c!=component) w-=c.width();

        return w;
    }
    public static double fillRowHeight(Component component, boolean isolated, double dfault){
        return isolated?dfault:Component.rowHeight(Component.getRow(component));
    }

    public static <T> boolean arrContains(T val, T[] arr){
        for(var v : arr) if(val.equals(v)) return true;
        return false;
    }
}