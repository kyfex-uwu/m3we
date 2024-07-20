package com.kyfexuwu.m3we.editor;

public class Vec2d {
    public double x;
    public double y;
    public Vec2d(double x, double y){
        this.x=x;
        this.y=y;
    }

    public Vec2d add(Vec2d pos) {
        return new Vec2d(this.x+pos.x,this.y+pos.y);
    }
}
