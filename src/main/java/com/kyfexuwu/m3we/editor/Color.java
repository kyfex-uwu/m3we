package com.kyfexuwu.m3we.editor;

import net.minecraft.client.render.VertexConsumer;

public record Color(int r, int g, int b) {
    public Color highlighted(){ return new Color(
            255-(int)((255-this.r)*0.7),255-(int)((255-this.g)*0.7),255-(int)((255-this.b)*0.7)); }
    public VertexConsumer setColor(VertexConsumer consumer){
        return consumer.color(this.r,this.g,this.b,255);
    }

    public int toInt() { return 0xff000000+this.r*0x10000+this.g*0x100+this.b; }
}
